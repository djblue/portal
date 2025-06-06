(ns portal.extensions.pwa
  (:require [clojure.string :as str]
            [examples.data :as demo]
            [portal.async :as a]
            [portal.colors :as c]
            [portal.extensions.vs-code-notebook :as notebook]
            [portal.resources :as io]
            [portal.runtime :as rt]
            [portal.runtime.edn :as edn]
            [portal.runtime.json :as json]
            [portal.runtime.transit :as transit]
            [portal.shortcuts :as shortcuts]
            [portal.ui.api :as api]
            [portal.ui.app :as app]
            [portal.ui.commands :as commands]
            [portal.ui.drag-and-drop :as dnd]
            [portal.ui.inspector :as ins]
            [portal.ui.state :as state]
            [portal.ui.styled :refer [div]]
            [portal.ui.theme :as theme]
            [reagent.core :as r]
            [reagent.dom :as dom]))

(defn open-demo
  "Load demo data"
  {:shortcuts [^::shortcuts/osx #{"meta" "d"}
               ^::shortcuts/windows ^::shortcuts/linux #{"control" "d"}]}
  [state]
  (state/dispatch! state state/history-push {:portal/value demo/data}))

(def commands
  [#'commands/open-command-palette
   #'commands/open-file
   #'commands/paste
   #'open-demo])

(doseq [var commands] (commands/register! var))

(defn send! [msg]
  (when-let [f (get rt/ops (:op msg))]
    (js/Promise.
     (fn [resolve]
       (f msg resolve)))))

(defn launch-queue-consumer [item]
  (a/let [files (js/Promise.all (map #(.getFile %) (.-files item)))
          value (dnd/handle-files files)]
    (state/dispatch! state/state state/history-push {:portal/value value})))

(defn handle-message [message]
  (let [data (.-data message)]
    (when-let [event (and (string? data) (js/JSON.parse data))]
      (case (.-type event)
        "close" (js/window.close)
        "set-title" (state/set-title (.-title event))
        "set-theme" (state/set-theme (.-color event))))))

(def hex-color #"#[0-9a-f]{6}")

(defn splash []
  (let [theme (theme/use-theme)
        state (state/use-state)
        mapping (reduce-kv
                 (fn [mapping k v]
                   (assoc mapping v (get theme k)))
                 {}
                 (::c/nord c/themes))
        svg (str/replace (io/inline "splash.svg") hex-color mapping)]
    [:<>
     [commands/palette]
     [div
      {:style
       {:display :flex
        :align-items :center
        :justify-content :center
        :height "100vh"
        :width "100vw"
        :border-top [1 :solid (::c/border theme)]}}
      [div
       {:style
        {:display :flex
         :flex-direction :column
         :align-items :center}}
       [div
        {:style {:width :fit-content
                 :margin "0 auto"}
         :dangerously-set-inner-HTML {:__html svg}}]
       [div
        {:style
         {:margin-top "10vh"
          :width "60vw"
          :max-width "800px"
          :min-width :fit-content
          :font-size "1.15em"
          :background (::c/background2 theme)
          :border [1 :solid (::c/border theme)]
          :border-radius (:border-radius theme)}}
        (for [command commands]
          ^{:key (hash command)}
          [div
           {:on-click #(command state)
            :style
            {:display :flex
             :align-items :center
             :justify-content :space-between
             :padding (:padding theme)
             :cursor :pointer
             :border-left [5 :solid "#0000"]}
            :style/hover
            {:background (::c/background theme)
             :border-left [5 :solid (::c/boolean theme)]}}
           [div
            {:style
             {:margin-left (:padding theme)}}
            (-> command meta :doc)]
           [commands/shortcut command]])]]]]))

(defn pwa-app []
  (if (contains? @state/state :portal/value)
    [app/app]
    [app/root
     [splash]]))

(def functional-compiler (r/create-compiler {:function-components true}))

(defn render-app []
  (when-let [el (.getElementById js/document "root")]
    (dom/render [pwa-app] el functional-compiler)))

(defn ->map [entries]
  (persistent!
   (reduce
    (fn [m entry]
      (assoc! m (keyword (aget entry 0)) (aget entry 1)))
    (transient {})
    entries)))

(defn- qs->map [qs] (->map (.entries (js/URLSearchParams. qs))))

(defn get-mode []
  (let [search (.. js/window -location -search (slice 1))
        params (qs->map search)]
    (cond
      (empty? params)       [:app]
      (:content-url params) [:fetch params]
      :else                 [:host search])))

(defn create-iframe [src]
  (let [frame (js/document.createElement "iframe")]
    (set! (.-style frame) "border: none; position: fixed; height: 100vh; width: 100vw")
    (set! (.-src frame) src)
    (set! (.-allow frame) "clipboard-read; clipboard-write")
    frame))

(defn host-mode [src]
  (js/window.addEventListener "message" #(handle-message %))
  (js/document.body.appendChild (create-iframe src)))

(defn- with-meta* [obj m]
  (if-not (implements? IMeta obj) obj (vary-meta obj merge m)))

(defn- parse-data [params response]
  (let [{:keys [status body]} response
        content-type          (some->
                               (or (:content-type params)
                                   (get-in response [:headers :content-type]))
                               (str/split #";")
                               first)
        metadata              {:query-params params :http-response response}]
    (if-not (= status 200)
      (ins/error->data
       (ex-info (str "Error fetching data: " status) metadata))
      (try
        (with-meta*
          (case content-type
            "application/transit+json" (transit/read body)
            "application/json"         (json/read body)
            "application/edn"          (edn/read-string body)
            "text/plain"               body
            (ins/error->data
             (ex-info (str "Unsupported :content-type " content-type) metadata)))
          metadata)
        (catch :default e
          (ins/error->data
           (ex-info (str "Error parsing :content-type " content-type) metadata e)))))))

(defn ->response [^js response]
  (a/let [body (.text response)]
    {:status  (.-status response)
     :body    body
     :headers (->map (.entries (.-headers response)))}))

(defn fetch-mode [params]
  (a/let [fetched  (js/fetch (:content-url params))
          response (->response fetched)
          value    (parse-data params response)]
    (reset! state/sender send!)
    (swap! state/state assoc :portal/value value)
    (render-app)))

(defn app-mode []
  (when (and js/navigator.serviceWorker
             (= js/window.location.host "djblue.github.io"))
    (js/navigator.serviceWorker.register "sw.js"))
  (when js/window.launchQueue
    (js/window.launchQueue.setConsumer
     #(launch-queue-consumer %)))
  (reset! state/sender send!)
  (render-app))

(defn main! []
  (let [[mode params] (get-mode)]
    (case mode
      :app   (app-mode)
      :fetch (fetch-mode params)
      :host  (host-mode params))))

(defn reload! []
  (let [[mode] (get-mode)]
    (when (= mode :app)
      (render-app))))

(set! (.-embed api/portal-api) notebook/activate)
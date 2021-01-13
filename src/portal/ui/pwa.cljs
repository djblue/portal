(ns portal.ui.pwa
  (:require [clojure.datafy :refer [datafy nav]]
            [clojure.string :as str]
            [examples.data :as demo]
            [portal.async :as a]
            [portal.colors :as c]
            [portal.resources :as io]
            [portal.shortcuts :as shortcuts]
            [portal.ui.app :as app]
            [portal.ui.commands :as commands]
            [portal.ui.drag-and-drop :as dnd]
            [portal.ui.state :as s :refer [state]]
            [portal.ui.styled :refer [div]]
            [reagent.dom :as rdom]))

(defn clipboard []
  (js/navigator.clipboard.readText))

(defn open-file []
  (js/Promise.
   (fn [resolve _reject]
     (let [id      "open-file-dialog"
           input   (or
                    (js/document.getElementById id)
                    (js/document.createElement "input"))
           accept   (->> dnd/handlers
                         keys
                         (map (partial str "."))
                         (str/join ", "))]
       (set! (.-id input) id)
       (set! (.-type input) "file")
       (set! (.-multiple input) "true")
       (set! (.-accept input) accept)
       (set! (.-style input) "visibility:hidden")
       (.addEventListener
        input
        "change"
        (fn [event]
          (a/let [value (dnd/handle-files (-> event .-target .-files))]
            (resolve value)))
        false)
       (js/document.body.appendChild input)
       (.click input)))))

(def ^:private fns
  {'portal.runtime/get-functions (constantly [])})

(def commands
  [commands/open-command-palette
   {:name 'portal.load/file
    :label "Open a File"
    ::shortcuts/osx #{"meta" "o"}
    ::shortcuts/default #{"control" "o"}
    :run #(a/let [value (open-file)]
            (s/dispatch % s/history-push {:portal/value value}))}
   {:name 'portal.load/clipboard
    :label "Load from clipboard"
    ::shortcuts/osx #{"meta" "v"}
    ::shortcuts/default #{"control" "v"}
    :run #(a/let [value (clipboard)]
            (s/dispatch % s/history-push {:portal/value value}))}
   {:name 'portal.load/demo
    :label "Load demo data"
    ::shortcuts/osx #{"meta" "d"}
    ::shortcuts/default #{"control" "d"}
    :run #(s/dispatch % s/history-push {:portal/value demo/data})}])

(defn invoke [{:keys [f args]} done]
  (try
    (let [f (if (symbol? f) (get fns f) f)]
      (done {:return (apply f args)}))
    (catch js/Error e
      (done {:return e}))))

(defn on-nav [request]
  (a/let [res (apply nav (:args request))]
    {:value (datafy res)}))

(defn send! [msg]
  (js/Promise.resolve
   (case (:op msg)
     :portal.rpc/clear-values nil
     :portal.rpc/on-nav (on-nav msg)
     :portal.rpc/invoke (invoke msg identity))))

(defn set-title [title]
  (set! (.-title js/document) title))

(defn launch-queue-consumer [settings item]
  (a/let [files (js/Promise.all (map #(.getFile %) (.-files item)))
          value (dnd/handle-files files)]
    (s/dispatch settings s/history-push {:portal/value value})))

(defn handle-message [message]
  (let [data (.-data message)]
    (when-let [event (and (string? data) (js/JSON.parse data))]
      (case (.-type event)
        "close" (js/window.close)
        "set-theme" (s/set-theme (.-color event))))))

(def hex-color #"#[0-9a-f]{6}")

(defn splash []
  (let [settings (merge (s/get-settings) {:send! send!})
        mapping (reduce-kv
                 (fn [mapping k v]
                   (assoc mapping v (get settings k)))
                 {}
                 (::c/nord c/themes))
        svg (str/replace (io/resource "splash.svg") hex-color mapping)]
    [dnd/area
     settings
     [app/root
      settings
      [commands/palette
       (assoc settings :commands commands)]
      [div
       {:style
        {:display :flex
         :align-items :center
         :justify-content :center
         :height "100vh"
         :width "100vw"}}
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
           :background (::c/background2 settings)
           :border [1 :solid (::c/border settings)]
           :border-radius (:border-radius settings)}}
         (for [command commands]
           ^{:key (hash command)}
           [div
            {:on-click #((:run command) settings)
             :style
             {:display :flex
              :align-items :center
              :justify-content :space-between
              :padding (:spacing/padding settings)
              :cursor :pointer
              :border-left [5 :solid "#0000"]}
             :style/hover
             {:background (::c/background settings)
              :border-left [5 :solid (::c/boolean settings)]}}
            [div
             {:style
              {:margin-left (:spacing/padding settings)}}
             (:label command)]
            [commands/shortcut settings command]])]]]]]))

(defn pwa-app []
  (if (contains? @state :portal/value)
    [app/app {:send! send!}]
    [splash]))

(defn render-app []
  (rdom/render [pwa-app]
               (.getElementById js/document "root")))

(defn get-mode []
  (let [src (js/location.search.slice 1)]
    (if (str/blank? src)
      [:app]
      [:host src])))

(defn create-iframe [src]
  (let [frame (js/document.createElement "iframe")]
    (set! (.-style frame) "border: 0; width: 100vw; height: calc(100vh + 1px)")
    (set! (.-src frame) src)
    frame))

(defn host-mode [src]
  (js/window.addEventListener "message" #(handle-message %))
  (js/document.body.appendChild (create-iframe src)))

(defn app-mode []
  (when js/navigator.serviceWorker
    (js/navigator.serviceWorker.register "sw.js"))
  (when js/window.launchQueue
    (js/window.launchQueue.setConsumer
     #(launch-queue-consumer (s/get-settings) %)))
  (render-app))

(defn main! []
  (let [[mode args] (get-mode)]
    (case mode
      :app (app-mode)
      :host (host-mode args))))

(defn reload! []
  (let [[mode] (get-mode)]
    (when (= mode :app)
      (render-app))))

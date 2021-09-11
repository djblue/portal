(ns portal.ui.pwa
  (:require [clojure.datafy :refer [datafy nav]]
            [clojure.string :as str]
            [examples.data :as demo]
            [portal.async :as a]
            [portal.colors :as c]
            [portal.resources :as io]
            [portal.ui.app :as app]
            [portal.ui.commands :as commands]
            [portal.ui.drag-and-drop :as dnd]
            [portal.ui.state :as state]
            [portal.ui.styled :refer [div]]
            [portal.ui.theme :as theme]
            [reagent.core :as r]
            [reagent.dom :as dom]))

(defn clipboard []
  (js/navigator.clipboard.readText))

(defn open-file []
  (js/Promise.
   (fn [resolve _reject]
     (let [id      "open-file-dialog"
           input   (or
                    (js/document.getElementById id)
                    (js/document.createElement "input"))]
       (set! (.-id input) id)
       (set! (.-type input) "file")
       (set! (.-multiple input) "true")
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
  {'clojure.datafy/nav           #'nav
   'clojure.datafy/datafy        #'datafy
   'portal.runtime/get-functions
   (fn []
     (keys
      (dissoc fns
              'clojure.datafy/nav
              'portal.runtime/get-functions)))})

(def commands
  [commands/open-command-palette
   {:name 'portal.load/file
    :label "Open a File"
    :run #(a/let [value (open-file)]
            (state/dispatch! % state/history-push {:portal/value value}))}
   {:name 'portal.load/clipboard
    :label "Load from clipboard"
    :run #(a/let [value (clipboard)]
            (state/dispatch! % state/history-push {:portal/value value}))}
   {:name 'portal.load/demo
    :label "Load demo data"
    :run #(state/dispatch! % state/history-push {:portal/value demo/data})}])

(defn invoke [{:keys [f args]} done]
  (try
    (let [f (if (symbol? f) (get fns f) f)]
      (done {:return (apply f args)}))
    (catch js/Error e
      (done {:return e}))))

(defn send! [msg]
  (js/Promise.resolve
   (case (:op msg)
     :portal.rpc/invoke (invoke msg identity))))

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
        svg (str/replace (io/resource "splash.svg") hex-color mapping)]
    [:<>
     [commands/palette {:commands commands}]
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
           {:on-click #((:run command) state)
            :style
            {:display :flex
             :align-items :center
             :justify-content :space-between
             :padding (:spacing/padding theme)
             :cursor :pointer
             :border-left [5 :solid "#0000"]}
            :style/hover
            {:background (::c/background theme)
             :border-left [5 :solid (::c/boolean theme)]}}
           [div
            {:style
             {:margin-left (:spacing/padding theme)}}
            (:label command)]
           [commands/shortcut command]])]]]]))

(defn pwa-app []
  (if (contains? @state/state :portal/value)
    [app/app]
    [app/root
     [dnd/area [splash]]]))

(def functional-compiler (r/create-compiler {:function-components true}))

(defn render-app []
  (dom/render [pwa-app]
              (.getElementById js/document "root")
              functional-compiler))

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
     #(launch-queue-consumer %)))
  (reset! state/sender send!)
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

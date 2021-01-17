(ns portal.ui.drag-and-drop
  (:require ["react" :as react]
            [cljs.reader :refer [read-string]]
            [portal.async :as a]
            [portal.ui.state :as state]
            [portal.ui.styled :as s]
            [portal.ui.viewer.markdown :as md]))

(defn read-file [file]
  (js/Promise.
   (fn [resolve reject]
     (let [reader (js/window.FileReader.)]
       (.addEventListener
        reader
        "load"
        (fn [e]
          (resolve (.-result (.-target e)))))
       (.addEventListener reader "error" reject)
       (.readAsText reader file)))))

(def handlers
  {"json" (fn [content] (js->clj (js/JSON.parse content)))
   "edn"  read-string
   "md"   (fn [content]
            (with-meta
              (md/parse-markdown content)
              {:portal.viewer/default :portal.viewer/hiccup}))})

(defn handle-file [file]
  (a/let [name    (.-name file)
          content (read-file file)
          [_ ext] (re-find #"\.(.+)$" name)
          handler (get handlers ext identity)]
    [name (handler content)]))

(defn handle-files [files]
  (a/let [value (js/Promise.all (map handle-file files))]
    (if (= (count value) 1)
      (second (first value))
      (into {} value))))

(defn area [settings children]
  (let [[active? set-active!] (react/useState false)]
    [s/div
     {:on-drag-over
      (fn [e]
        (.preventDefault e)
        (set-active! true))
      :on-drop
      (fn [e]
        (.preventDefault e)
        (a/let [value (handle-files
                       (for [item (.-dataTransfer.items e)
                             :when (= (.-kind item) "file")]
                         (.getAsFile item)))]
          (state/dispatch settings state/history-push {:portal/value value}))
        (set-active! false))
      :style {:position :relative}}
     (when active?
       [:<>
        [s/div
         {:style
          {:position :absolute
           :top 0
           :left 0
           :right 0
           :bottom 0
           :padding 40
           :box-sizing :border-box
           :color "white"
           :background "rgba(0,0,0,0.5)"}}
         [s/div
          {:style
           {:user-select :none
            :border "5px dashed white"
            :border-radius 20
            :height "calc(100% - 10px)"
            :display :flex
            :justify-content :center
            :align-items :center}}
          [:h1 {:style {:font-family "sans-serif"}} "Drag & Drop Your File Here"]]]
        [s/div
         {:style
          {:position :absolute
           :top 0
           :left 0
           :right 0
           :bottom 0}
          :on-drag-leave
          (fn [_e]
            (set-active! false))}]])
     children]))

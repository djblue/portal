(ns ^:no-doc portal.ui.drag-and-drop
  (:require ["react" :as react]
            [cljs.reader :refer [read-string]]
            [clojure.string :as string]
            [portal.async :as a]
            [portal.ui.state :as state]
            [portal.ui.styled :as s]
            [portal.ui.viewer.csv :as csv]
            [portal.ui.viewer.markdown :as md]))

(defn read-file
  ([file]
   (read-file file :text))
  ([file type]
   (js/Promise.
    (fn [resolve reject]
      (let [reader (js/window.FileReader.)]
        (.addEventListener
         reader
         "load"
         (fn [e]
           (resolve (.-result (.-target e)))))
        (.addEventListener reader "error" reject)
        (case type
          :text (.readAsText reader file)
          :bin  (.readAsArrayBuffer reader file)))))))

(defn- read-binary
  [file]
  (a/let [content (read-file file :bin)]
    (js/Uint8Array. content)))

(defn- read-html
  [file]
  (a/let [content (read-file file)]
    (with-meta
      [:portal.viewer/html content]
      {:portal.viewer/default :portal.viewer/hiccup})))

(def handlers
  {"json" (fn [file]
            (a/let [content (read-file file)]
              (js->clj (js/JSON.parse content))))
   "edn"  (fn [file]
            (a/let [content (read-file file)]
              (read-string content)))
   "md"   (fn [file]
            (a/let [content (read-file file)]
              (with-meta
                (md/parse-markdown content)
                {:portal.viewer/default :portal.viewer/hiccup})))
   "csv"  (fn [file]
            (a/let [content (read-file file)]
              (with-meta
                (csv/parse-csv content)
                {:portal.viewer/default :portal.viewer/table})))
   "txt"  read-file
   "svg"  read-html
   "htm"  read-html
   "html" read-html
   "png"  read-binary
   "jpg"  read-binary
   "jpeg" read-binary
   "gif"  read-binary
   "pdf"  read-binary})

(defn handle-file [file]
  (a/let [name    (.-name file)
          [_ ext] (re-find #"\.(.+)$" name)
          ext     (when ext (string/lower-case ext))
          handler (get handlers ext read-file)
          content (handler file)]
    [name content]))

(defn handle-files [files]
  (a/let [value (js/Promise.all (map handle-file files))]
    (if (= (count value) 1)
      (second (first value))
      (into {} value))))

(defn area [children]
  (let [state                 (state/use-state)
        [active? set-active!] (react/useState false)]
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
          (state/dispatch! state state/history-push {:portal/value value}))
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

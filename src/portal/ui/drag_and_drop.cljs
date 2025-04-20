(ns ^:no-doc portal.ui.drag-and-drop
  (:require [clojure.string :as string]
            [portal.async :as a]
            [portal.ui.parsers :as p]
            [portal.ui.react :as react]
            [portal.ui.state :as state]
            [portal.ui.styled :as s]
            [portal.viewer :as v]))

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
    (v/hiccup [:portal.viewer/html content])))

(def handlers
  {"json" (fn [file]
            (a/let [content (read-file file)]
              (p/parse-string :format/json content)))
   "edn"  (fn [file]
            (a/let [content (read-file file)]
              (p/parse-string :format/edn content)))
   "md"   (fn [file]
            (a/let [content (read-file file)]
              (v/hiccup (p/parse-string :format/md content))))
   "csv"  (fn [file]
            (a/let [content (read-file file)]
              (v/table (p/parse-string :format/csv content))))
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
        [active? set-active!] (react/use-state false)]
    [s/div
     {:on-click #(set-active! false)
      :on-drag-over
      (fn [e]
        (.preventDefault e)
        (set-active! true))
      :on-drop
      (fn [e]
        (.preventDefault e)
        (a/let [value (handle-files
                       (for [item (.items (.-dataTransfer e))
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
           :background "rgba(0,0,0,0.5)"
           :z-index 100}}
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
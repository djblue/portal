(ns ^:no-doc portal.ui.viewer.table
  (:require [clojure.spec.alpha :as s]
            [portal.colors :as c]
            [portal.ui.filter :as f]
            [portal.ui.inspector :as ins]
            [portal.ui.lazy :as l]
            [portal.ui.react :as react]
            [portal.ui.select :as select]
            [portal.ui.styled :as d]
            [portal.ui.theme :as theme]
            [reagent.core :as r]))

(defonce ^:private hover (react/create-context nil))

(defn- with-hover [& children]
  (r/with-let [value (r/atom nil)]
    (into [:r> (.-Provider hover) #js {:value value}] children)))

(defn- use-hover [] (react/use-context hover))

(defn- hover? [hover selector value] (= (selector @hover) value))

(defn- table [& children]
  (let [theme   (theme/use-theme)
        context (ins/use-context)]
    (into
     [d/div
      {:style
       (cond->
        {:display :grid
         :grid-gap 1
         :font-size (:font-size theme)
         :background (::c/border theme)
         :border [1 :solid (::c/border theme)]
         :border-radius (:border-radius theme)
         :grid-template-columns :min-content}
         (not= (:depth context) 1)
         (assoc :overflow :auto))}]
     children)))

(defn- cell [row column child]
  (let [background (ins/get-background)
        theme      (theme/use-theme)
        width      2
        border     (ins/get-background2)
        hover      (use-hover)]
    [d/div
     {:on-mouse-over
      (fn []
        (reset! hover [row column]))
      :style
      {:background  background
       :grid-row    (str (inc row))
       :grid-column (str (inc column))}}
     [d/div
      {:style (cond->
               {:height     "100%"
                :width      "100%"
                :box-sizing :border-box
                :padding    (:padding theme)}
                (nil? child)
                (assoc
                 :background
                 (list
                  'repeating-linear-gradient
                  "45deg"
                  [border 0]
                  [border width]
                  [background width]
                  [background (* 3 width)])))
       :style/hover
       {:background (when child (str (::c/border theme) "55"))}}
      [select/with-position {:row row :column column} child]]]))

(defn- special [row column child span]
  (let [background (ins/get-background)
        theme      (theme/use-theme)
        hover      (use-hover)]
    [d/div
     {:style
      (merge
       (cond
         (= 0 row column)
         {:z-index 3
          :top 0
          :left 0
          :border-bottom [3 :solid (::c/border theme)]
          :border-right [3 :solid (::c/border theme)]}
         (zero? row)
         {:z-index 2
          :top 0
          :text-align :center
          :border-bottom [3 :solid (::c/border theme)]}
         (zero? column)
         {:z-index 1
          :left 0
          :text-align :right
          :border-right [3 :solid (::c/border theme)]})
       (cond
         @(r/track hover? hover first row)
         {:border-right [3 :solid (::c/boolean theme)]}
         @(r/track hover? hover second column)
         {:border-bottom [3 :solid (::c/boolean theme)]})
       {:position :sticky
        :background background
        :box-sizing :border-box
        :padding (:padding theme)
        :grid-row (str (inc row)
                       (when span (str " / span " span)))
        :grid-column (str (inc column))})}
     [select/with-position {:row row :column column} child]]))

(defn- columns [cols]
  [:<>
   [special 0 0]
   (map-indexed
    (fn [col-index column]
      ^{:key (hash column)}
      [ins/with-key column
       [special 0 (inc col-index) [ins/inspector column]]])
    cols)])

(defn- inspect-map-table [values]
  (let [rows (seq (ins/try-sort (keys values)))
        cols (or (get-in (meta values) [:portal.viewer/table :columns])
                 (seq (ins/try-sort (into #{} (mapcat keys (vals values))))))
        search-text (ins/use-search-text)
        matcher     (f/match search-text)]
    [table
     [columns cols]
     [l/lazy-seq
      (keep-indexed
       (fn [row-index row]
         (when (or (matcher row)
                   (matcher (get values row)))
           [:<>
            {:key (hash row)}
            [ins/with-key row
             [special (inc row-index) 0 [ins/inspector row]]]
            [ins/toggle-bg
             [ins/with-key row
              (map-indexed
               (fn [col-index column]
                 (let [coll (get values row)]
                   ^{:key col-index}
                   [ins/with-collection coll
                    [ins/with-key column
                     [cell
                      (inc row-index)
                      (inc col-index)
                      (when (contains? coll column)
                        [ins/inspector (ins/get-props coll column) (get coll column)])]]]))
               cols)]]]))
       rows)]]))

(defn- inspect-coll-table [values]
  (let [rows (seq values)
        cols (or (get-in (meta values) [:portal.viewer/table :columns])
                 (seq (ins/try-sort (into #{} (mapcat keys values)))))
        search-text (ins/use-search-text)
        matcher     (f/match search-text)]
    [table
     [columns cols]
     [l/lazy-seq
      (keep-indexed
       (fn [row-index row]
         (when (matcher row)
           ^{:key row-index}
           [:<>
            [ins/with-key row-index
             [special (inc row-index) 0 [ins/inspector row-index]]]
            [ins/toggle-bg
             [ins/with-key row-index
              (map-indexed
               (fn [col-index column]
                 ^{:key col-index}
                 [ins/with-collection row
                  [ins/with-key column
                   [cell
                    (inc row-index)
                    (inc col-index)
                    (when (contains? row column)
                      [ins/inspector (ins/get-props row column) (get row column)])]]])
               cols)]]]))
       rows)]]))

(defn- inspect-vector-table [values]
  (let [search-text (ins/use-search-text)
        matcher     (f/match search-text)]
    [table
     (when-let [cols (get-in (meta values) [:portal.viewer/table :columns])]
       [columns cols])
     [l/lazy-seq
      (keep-indexed
       (fn [row-index row]
         (when (matcher row)
           ^{:key row-index}
           [:<>
            [ins/with-key row-index
             [special (inc row-index) 0 [ins/inspector row-index]]]
            [ins/toggle-bg
             [ins/with-key row-index
              (map-indexed
               (fn [col-index value]
                 ^{:key col-index}
                 [ins/with-collection row
                  [ins/with-key col-index
                   [cell
                    (inc row-index)
                    (inc col-index)
                    [ins/inspector value]]]])
               row)]]]))
       values)]]))

(defn- inspect-map [values]
  (let [rows (seq (ins/try-sort (keys values)))
        search-text (ins/use-search-text)
        matcher     (f/match search-text)]
    [table
     [l/lazy-seq
      (keep-indexed
       (fn [row-index row]
         (when (or (matcher row)
                   (matcher (get values row)))
           [:<>
            {:key row-index}
            [ins/with-key row
             [special (inc row-index) 0 [ins/inspector row]]]
            [ins/toggle-bg
             [ins/with-key row
              [ins/with-collection values
               [ins/with-key row-index
                [ins/with-key row
                 [cell
                  (inc row-index)
                  1
                  [ins/inspector (ins/get-props values row) (get values row)]]]]]]]]))
       rows)]]))

(defn- inspect-multi-map-table [values]
  (let [rows (seq (ins/try-sort (keys values)))
        cols (or (get-in (meta values) [:portal.viewer/table :columns])
                 (seq (ins/try-sort (into #{} (comp (mapcat second) (mapcat keys)) values))))
        search-text (ins/use-search-text)
        matcher     (f/match search-text)]
    [table
     [columns cols]
     [l/lazy-seq
      (keep-indexed
       (fn [row-index {:keys [row value index]}]
         (when (or (matcher row) (matcher value))
           [:<>
            {:key row-index}
            (when (zero? index)
              [ins/with-key row
               [special (inc row-index) 0 [ins/inspector row] (count (get values row))]])
            [ins/toggle-bg
             [ins/with-key row
              (map-indexed
               (fn [col-index column]
                 ^{:key col-index}
                 [ins/with-collection value
                  [ins/with-key index
                   [ins/with-key column
                    [cell
                     (inc row-index)
                     (inc col-index)
                     (when (contains? value column)
                       [ins/inspector (ins/get-props value column) (get value column)])]]]])
               cols)]]]))
       (mapcat
        (fn [row]
          (map-indexed
           (fn [index value]
             {:row row :value value :index index})
           (get values row)))
        rows))]]))

;;; :spec
(s/def ::map            map?)
(s/def ::coll-of-maps   (s/coll-of map?))
(s/def ::multi-map      (s/map-of any? ::coll-of-maps))
(s/def ::map-of-maps    (s/map-of any? map?))
(s/def ::coll-of-vector (s/coll-of vector?))

(s/def ::table
  (s/or :multi-map      ::multi-map
        :map-of-maps    ::map-of-maps
        :map            ::map
        :coll-of-maps   ::coll-of-maps
        :coll-of-vector ::coll-of-vector))
;;;

(defn- get-component [value]
  (let [result (s/conform ::table value)]
    (when-not (= result ::s/invalid)
      (case (first result)
        :multi-map      inspect-multi-map-table
        :map-of-maps    inspect-map-table
        :map            inspect-map
        :coll-of-vector inspect-vector-table
        :coll-of-maps   inspect-coll-table))))

(defn table-view? [value] (s/valid? ::table value))

(defn inspect-table [values]
  (let [component (get-component values)]
    [ins/with-collection values
     [with-hover
      [component values]]]))

(def viewer
  {:predicate table-view?
   :component #'inspect-table
   :name :portal.viewer/table
   :doc "View value as a table. Supports sticky headers and keyboard navigation."})

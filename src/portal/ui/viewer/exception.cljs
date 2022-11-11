(ns portal.ui.viewer.exception
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [portal.colors :as c]
            [portal.ui.icons :as icon]
            [portal.ui.inspector :as ins]
            [portal.ui.styled :as d]
            [portal.ui.theme :as theme]
            [portal.ui.viewer.log :as log]
            [reagent.core :as r]))

;;; :spec
(s/def ::cause string?)

(s/def ::trace-line
  (s/cat :class symbol?
         :method symbol?
         :file (s/or :str string? :nil nil?)
         :line number?))

(s/def ::trace (s/coll-of ::trace-line :min-count 1))

(s/def ::type symbol?)
(s/def ::message string?)
(s/def ::at ::trace-line)

(s/def ::via
  (s/coll-of
   (s/keys :req-un [::type]
           :opt-un [::message ::at])))

(s/def ::exception
  (s/keys :req-un [::via]
          :opt-un [::trace ::cause]))
;;;

(defn trace? [value]
  (s/valid? ::trace value))

(defn exception? [value]
  (s/valid? ::exception value))

(defn- inspect-sub-trace [trace]
  (r/with-let [expanded? (r/atom (zero? (:index (first trace))))]
    (let [theme (theme/use-theme)
          {:keys [clj? sym class file]} (first trace)]
      [:<>
       [(if @expanded?
          icon/chevron-down
          icon/chevron-right)
        {:size "sm"
         :style
         {:grid-column "1"
          :width       (* 3 (:padding theme))
          :color       (::c/border theme)}}]
       [d/div
        {:on-click (fn [e]
                     (swap! expanded? not)
                     (.stopPropagation e))
         :style
         {:grid-column "2"
          :cursor      :pointer}}

        [d/span
         {:title file
          :style {:color (if clj?
                           (::c/namespace theme)
                           (::c/package theme))}}
         (if-not clj? class (namespace sym))]]
       [d/span
        {:style
         {:grid-column "3"
          :color       (::c/border theme)}}
        " [" (count trace) "]"]
       (when @expanded?
         (for [{:keys [clj? sym method line index]} trace]
           [:<>
            {:key index}
            [d/div]
            [d/div
             (if clj?
               (name sym)
               [d/div method])]
            [ins/inspector line]]))])))

(defn- analyze-trace-item [index trace]
  (let [[class method file line] trace
        clj-name (demunge class)
        clj? (or (and (string? file)
                      (str/ends-with? file ".clj"))
                 (not= clj-name class))]
    (merge
     {:class  class
      :method method
      :file   file
      :line   line
      :index  index}
     (when clj?
       {:clj? true
        :sym  clj-name}))))

(defn- inspect-stack-trace [trace]
  (let [theme   (theme/use-theme)
        options (ins/use-options)]
    [d/div
     {:style
      {:background            (ins/get-background)
       :box-sizing            :border-box
       :padding               (:padding theme)
       :display               :grid
       :grid-template-columns "auto 1fr auto"
       :align-items           :center
       :grid-gap              [0 (:padding theme)]
       :border-radius         (:border-radius theme)
       :border                [1 :solid (::c/border theme)]}}
     (->> trace
          (map-indexed
           analyze-trace-item)
          (filter
           (fn [{:keys [clj?]}]
             (if (:expanded? options) true clj?)))
          (partition-by :file)
          (map
           (fn [trace]
             ^{:key (hash trace)}
             [inspect-sub-trace trace])))]))

(defn- inspect-via [value]
  (let [theme                  (theme/use-theme)
        {:keys [type message]} (last (:via value))
        message                (or (:cause value) message)]
    [d/div
     {:style
      {:display         :flex
       :justify-content :space-between
       :align-items     :center
       :box-sizing      :border-box}}
     [d/div
      {:style
       {:font-weight :bold
        :white-space :pre-bold
        :color (::c/exception theme)
        :padding [(:padding theme) (* 2 (:padding theme))]}}
      (if message
        message
        (pr-str (:phase value type)))]
     [d/div
      {:style {:display         :flex
               :align-items     :center
               :justify-content :space-between}}
      [d/div
       {:style {:padding [(:padding theme) (* 2 (:padding theme))]}}
       (when message (pr-str (:phase value type)))]
      (when-let [value (:runtime value)]
        [d/div
         {:style {:padding     (:padding theme)
                  :display     :flex
                  :align-items :center
                  :box-sizing  :border-box
                  :color       (::c/exception theme)
                  :border-left [1 :solid (::c/exception theme)]}}
         [log/icon value (::c/exception theme)]])]]))

(defn inspect-exception [value]
  (let [theme     (theme/use-theme)
        options   (ins/use-options)
        expanded? (:expanded? options)]
    [d/div
     {:style
      {:color (::c/text theme)
       :font-size  (:font-size theme)}}
     [d/div
      [d/div
       {:style
        {:display    :flex
         :position   :relative
         :color      (::c/exception theme)
         :border     [1 :solid (::c/exception theme)]
         :border-top-right-radius    (:border-radius theme)
         :border-top-left-radius     (:border-radius theme)
         :border-bottom-right-radius (when-not expanded? (:border-radius theme))
         :border-bottom-left-radius  (when-not expanded? (:border-radius theme))}}
       [d/div
        {:style
         {:position :absolute
          :top 0
          :bottom 0
          :right 0
          :left 0
          :opacity 0.15
          :background (::c/exception theme)}}]
       [d/div
        {:style
         {:display      :flex
          :align-items  :center
          :padding      [0 (:padding theme)]
          :border-right [1 :solid (::c/exception theme)]}}
        [icon/exclamation-triangle {:size "lg"}]]
       [d/div
        {:style {:flex "1"}}
        [inspect-via value]]]
      (when expanded?
        [ins/with-collection
         value
         [ins/inspect-map-k-v (dissoc value :cause :phase :runtime)]])]]))

(def viewer
  {:predicate exception?
   :component inspect-exception
   :name      :portal.viewer/ex})

(def trace-viewer
  {:predicate trace?
   :component inspect-stack-trace
   :name      :portal.viewer/stack-trace})

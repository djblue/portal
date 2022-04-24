(ns portal.ui.viewer.exception
  (:require [clojure.spec.alpha :as spec]
            [clojure.string :as str]
            [portal.colors :as c]
            [portal.ui.icons :as icon]
            [portal.ui.inspector :as ins]
            [portal.ui.styled :as s]
            [portal.ui.theme :as theme]
            [reagent.core :as r]))

(spec/def ::cause string?)

(spec/def ::trace-line
  (spec/cat :class symbol?
            :method symbol?
            :file (spec/or :str string? :nil nil?)
            :line number?))

(spec/def ::trace (spec/coll-of ::trace-line :min-count 1))

(spec/def ::type symbol?)
(spec/def ::message string?)
(spec/def ::at ::trace-line)

(spec/def ::via
  (spec/coll-of
   (spec/keys :req-un [::type]
              :opt-un [::message ::at])))

(spec/def ::exception
  (spec/keys :req-un [::via]
             :opt-un [::trace ::cause]))

(defn trace? [value]
  (spec/valid? ::trace value))

(defn exception? [value]
  (spec/valid? ::exception value))

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
       [s/div
        {:on-click (fn [e]
                     (swap! expanded? not)
                     (.stopPropagation e))
         :style
         {:grid-column "2"
          :cursor      :pointer}}

        [s/span
         {:title file
          :style {:color (if clj?
                           (::c/namespace theme)
                           (::c/package theme))}}
         (if-not clj? class (namespace sym))]]
       [s/span
        {:style
         {:grid-column "3"
          :color       (::c/border theme)}}
        " [" (count trace) "]"]
       (when @expanded?
         (for [{:keys [clj? sym method line index]} trace]
           [:<>
            {:key index}
            [s/div]
            [s/div
             (if clj?
               (name sym)
               [s/div method])]
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
    [s/div
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
    [s/div
     {:style
      {:display         :flex
       :justify-content :space-between
       :box-sizing      :border-box
       :padding         (:padding theme)}}
     (when message
       [s/div
        {:style
         {:font-weight :bold
          :color (::c/exception theme)}}
        message])
     (pr-str (:phase value type))]))

(defn inspect-exception [value]
  (let [theme     (theme/use-theme)
        options   (ins/use-options)
        expanded? (:expanded? options)]
    [s/div
     {:style
      {:color (::c/text theme)
       :font-size  (:font-size theme)}}
     [s/div
      [s/div
       {:style
        {:display :flex
         :color      (::c/exception theme)
         :border     [1 :solid (::c/exception theme)]
         :background (str (::c/exception theme) "22")
         :border-top-right-radius    (:border-radius theme)
         :border-top-left-radius     (:border-radius theme)
         :border-bottom-right-radius (when-not expanded? (:border-radius theme))
         :border-bottom-left-radius  (when-not expanded? (:border-radius theme))}}
       [s/div
        {:style
         {:padding    (:padding theme)
          :border-right     [1 :solid (::c/exception theme)]}}
        [icon/terminal {:size "sm"}]]
       [s/div
        {:style {:flex "1"}}
        [inspect-via value]]]
      (when expanded?
        [ins/inspect-map-k-v (dissoc value :cause :phase)])]]))

(def viewer
  {:predicate exception?
   :component inspect-exception
   :name      :portal.viewer/ex})

(def trace-viewer
  {:predicate trace?
   :component inspect-stack-trace
   :name      :portal.viewer/stack-trace})

(ns ^:no-doc portal.ui.viewer.exception
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [portal.colors :as c]
            [portal.ui.filter :as f]
            [portal.ui.icons :as icon]
            [portal.ui.inspector :as ins]
            [portal.ui.select :as select]
            [portal.ui.styled :as d]
            [portal.ui.theme :as theme]
            [portal.ui.viewer.log :as log]
            [portal.viewer :as v]))

;;; :spec
(s/def ::cause string?)

(s/def ::trace-line
  (s/cat :class  (s/or :symbol symbol? :string string?)
         :method (s/or :symbol symbol? :string string?)
         :file   (s/or :str string? :nil nil?)
         :line   number?))

(s/def ::trace (s/coll-of ::trace-line :min-count 1))

(s/def ::type (s/or :symbol symbol? :string string?))
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
  (let [trace (map meta trace)
        theme               (theme/use-theme)
        context             (ins/use-context)
        {:keys [expanded? selected]} (ins/use-options)
        background                   (ins/get-background)
        {:keys [clj? sym class]}     (first trace)
        border (if selected
                 (get theme (nth theme/order selected))
                 (::c/border theme))
        can-expand? (> (count trace) 1)
        wrapper-options (ins/use-wrapper-options context)
        search-text (ins/use-search-text)
        matcher     (f/match search-text)]
    [:<>
     [d/div
      {:on-click (:on-click wrapper-options)
       :on-double-click (:on-double-click wrapper-options)
       :style {:grid-column "1"}}
      [d/div
       {:style
        {:background                background
         :display                   :flex
         :align-items               :center
         :position                  :relative
         :cursor                    :pointer
         :padding                   [(* 0.5 (:padding theme)) (:padding theme)]
         :border-top-left-radius    (:border-radius theme)
         :border-bottom-left-radius (:border-radius theme)
         :border-left               [1 :solid border]
         :border-top                [1 :solid border]
         :border-bottom             [1 :solid border]}}

       [d/div
        {:style
         {:width      1
          :position   :absolute
          :top        0
          :right      -1
          :bottom     0
          :background background}}]

       [d/div
        {:style {:width "1em"}}
        (when can-expand? [ins/toggle-expand])]

       [theme/with-theme+
        {::c/symbol (if clj?
                      (::c/namespace theme)
                      (::c/package theme))}
        [select/with-position
         {:row 0 :column 0}
         [ins/inspector (symbol (if-not clj? class (namespace sym)))]]]]]
     [d/div
      {:on-click (:on-click wrapper-options)
       :on-double-click (:on-double-click wrapper-options)
       :style
       {:grid-column "2"
        :display :flex
        :gap (:padding theme)
        :background                 background
        :padding                    [(* 0.5 (:padding theme)) (:padding theme)]
        :border                [1 :solid border]
        :border-top-right-radius    (:border-radius theme)
        :border-bottom-right-radius (:border-radius theme)
        :border-bottom-left-radius (when (and can-expand? expanded?) (:border-radius theme))}}
      [d/div
       {:style {:flex "1" :display :flex :flex-direction :column}}
       (keep-indexed
        (fn [idx {:keys [clj? sym method index] :as source}]
          (when (matcher method)
            ^{:key index}
            [ins/with-key
             index
             [d/div
              {:key idx
               :style {:display :flex
                       :flex "1"
                       :justify-content :space-between}}
              [d/div
               [select/with-position
                {:row idx :column 1}
                [ins/inspector {:portal.viewer/default :portal.viewer/source-location}
                 (assoc source :label (if clj?  (symbol nil (name sym)) method))]]]]]))
        (if expanded? trace (take 1 trace)))]
      [d/div
       {:style
        {:min-width "3em" :text-align :right}}
       (when can-expand?
         [d/span
          {:style
           {:color       (::c/border theme)}}
          " [" (count trace) "]"])]]]))

(defn- analyze-trace-item [index trace]
  (let [[class method file line] trace
        class (cond-> class (string? class) symbol)
        clj-name (demunge class)
        clj? (or (and (string? file)
                      (or (str/ends-with? file ".clj")
                          (str/ends-with? file ".cljs")
                          (str/ends-with? file ".cljc")))
                 (not= clj-name class))]
    (with-meta
      trace
      (merge
       {:class  class
        :method method
        :line   line
        :column 1
        :index  index}
       (when file
         {:file file})
       (when-let [ns (and clj? (some-> clj-name namespace symbol))]
         {:clj? true :ns ns :sym clj-name})))))

(defn- wrapper [context & children]
  (let [opts   (ins/use-options)
        viewer (get-in opts [:viewer :name])]
    (if (= viewer :portal.viewer/sub-trace)
      (into [:<>] children)
      [d/div
       {:style {:grid-column "1 / 3"}}
       (into [ins/wrapper context] children)])))

(defn- inspect-stack-trace [trace]
  (let [theme   (theme/use-theme)
        options (ins/use-options)
        search-text (ins/use-search-text)
        matcher     (f/match search-text)]
    [d/div
     {:style
      {:border-top-left-radius (:border-radius theme)
       :border-bottom-left-radius (:border-radius theme)
       :border-left [5 :solid (::c/exception theme)]}}
     [d/div
      {:style
       {:background            (ins/get-background)
        :box-sizing            :border-box
        :padding               (:padding theme)
        :display               :grid
        :grid-template-columns "auto 1fr"
        :grid-gap              [(* 0.5 (:padding theme)) 0]
        :border-top-right-radius (:border-radius theme)
        :border-bottom-right-radius (:border-radius theme)
        :border-right         [1 :solid (::c/border theme)]
        :border-top           [1 :solid (::c/border theme)]
        :border-bottom        [1 :solid (::c/border theme)]}}
      (->> trace
           (map-indexed
            analyze-trace-item)
           (filter
            (fn [line]
              (when (matcher line)
                (let [{:keys [clj? ns]} (meta line)]
                  (if (:expanded? options) true
                      (and clj? (not (or
                                      (str/starts-with? (str ns) "nrepl.middleware")
                                      (str/starts-with? (str ns) "clojure.lang.Compiler")))))))))
           (partition-by (comp :file meta))
           (map-indexed
            (fn [index trace]
              ^{:key index}
              [select/with-position
               {:column 0 :row index}
               [ins/with-key
                index
                [ins/inspector
                 {:portal.viewer/inspector {:wrapper wrapper}
                  :portal.viewer/default :portal.viewer/sub-trace}
                 (with-meta trace (meta (first trace)))]]])))]]))

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
        :white-space :pre-wrap
        :color (::c/exception theme)
        :padding [(:padding theme) (* 2 (:padding theme))]}}
      [ins/highlight-words
       (if message
         message
         (pr-str (:phase value type)))]]
     [d/div
      {:style {:display         :flex
               :align-items     :stretch
               :justify-content :space-between}}
      [d/div
       {:style {:display     :flex
                :align-items :center
                :padding [(:padding theme) (* 2 (:padding theme))]}}
       [ins/highlight-words
        (when message (str (:phase value type)))]]
      (when-let [value (:runtime value)]
        [d/div
         {:style {:padding     (* 0.5 (:padding theme))
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
       [ins/toggle-expand {:style {:z-index 50 :padding-left (:padding theme)}}]
       [d/div
        {:style {:flex "1"}}
        [inspect-via value]]]
      (when expanded?
        [ins/with-collection
         value
         [ins/inspect-map-k-v
          (v/for
           (dissoc value :cause :phase :runtime)
            {:trace :portal.viewer/stack-trace})]])]]))

(def viewer
  {:predicate exception?
   :component #'inspect-exception
   :name      :portal.viewer/ex
   :doc       "Viewer for datafied exceptions."})

(def trace-viewer
  {:predicate trace?
   :component #'inspect-stack-trace
   :name      :portal.viewer/stack-trace})

(def sub-trace-viewer
  {:predicate trace?
   :component #'inspect-sub-trace
   :name      :portal.viewer/sub-trace})

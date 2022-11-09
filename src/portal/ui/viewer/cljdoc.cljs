(ns portal.ui.viewer.cljdoc
  (:require ["react" :as react]
            [portal.colors :as c]
            [portal.ui.inspector :as ins]
            [portal.ui.select :as select]
            [portal.ui.styled :as s]
            [portal.ui.theme :as theme]))

(def ^:private observer-context (react/createContext nil))

(defn- with-observer [f & children]
  (let [[observer set-observer!] (react/useState nil)]
    (react/useEffect
     (fn []
       (set-observer!
        (js/IntersectionObserver.
         (fn [entries]
           (f
            (reduce
             (fn [result entry]
               (let [element (.-target entry)
                     id      (.getAttribute element "data-observer")]
                 (assoc result
                        id
                        {:element element
                         :ratio   (.-intersectionRatio entry)})))
             {}
             entries)))
         #js {:root nil :rootMargin "0px" :threshold 0})))
     #js [f])
    (when observer
      (into [:r>
             (.-Provider observer-context)
             #js {:value observer}]
            children))))

(defn- use-observer ^js [id]
  (let [ref      (react/useRef nil)
        observer (react/useContext observer-context)
        id       (str id)]
    (react/useEffect
     (fn []
       (when-let [el (.-current ref)]
         (.setAttribute el "data-observer" id)
         (.observe observer el)
         #(.unobserve observer el)))
     #js [id ref observer])
    ref))

(def ^:private index-context (react/createContext 0))

(defn- use-index [] (react/useContext index-context))

(defn- with-index [index & children]
  (into [:r> (.-Provider index-context) #js {:value index}] children))

(defn- first-visible [visible index]
  (->> visible
       (keep
        (fn [[label {:keys [ratio]}]]
          (when-not (zero? ratio) label)))
       (select-keys (:order index))
       (sort-by second)
       ffirst))

(defn- docs-nav [value visible]
  (let [index      (use-index)
        selected   (first-visible visible index)
        theme      (theme/use-theme)
        has-label? (string? (first value))
        label      (when has-label? (first value))
        value      (if-not has-label? value (rest value))
        parent     (get-in index [:parents selected])]
    [:<>
     (when (string? label)
       [s/div
        {:on-click
         (fn [e]
           (.stopPropagation e)
           (when-let [el (get-in visible [label :element])]
             (.scrollIntoView ^js el)))
         :style
         {:cursor        :pointer
          :font-size     "0.85rem"
          :color         (condp = label
                           parent   (::c/namespace theme)
                           selected (::c/boolean theme)
                           (::c/border theme))
          :box-sizing    :border-box
          :padding-right (* 4 (:padding theme))
          :border-right  [3 :solid (condp = label
                                     parent   (::c/namespace theme)
                                     selected (::c/boolean theme)
                                     "rgba(0,0,0,0)")]}}
        label])
     (when (and (coll? value) (not (map? value)))
       (for [child value]
         ^{:key (hash child)}
         [s/div
          {:style
           {:padding-left (* 3 (:padding theme))}}
          [docs-nav child visible selected]]))]))

(defn- render-article [value]
  (let [index         (use-index)
        theme         (theme/use-theme)
        last-label    (:last index)
        background    (ins/get-background)
        [label entry] value
        ref           (use-observer label)]
    [s/div
     {:style {:background  background
              :padding-top 1}}
     [s/section
      {:id    (:file entry label)
       :ref   ref
       :style {:border-bottom
               (when-not (= label last-label)
                 [1 :solid (::c/border theme)])
               :min-height
               (if (= label last-label) "calc(100vh - 226px)" :auto)}}
      [ins/with-key
       label
       [select/with-position
        {:row (get-in index [:order label]) :column 0}
        (or
         (when-let [markdown (:markdown entry)]
           [ins/dec-depth
            [ins/with-default-viewer
             :portal.viewer/markdown
             [ins/inspector markdown]]])
         (when-let [hiccup (:hiccup entry)]
           [ins/dec-depth
            [ins/with-default-viewer
             :portal.viewer/hiccup
             [ins/with-collection
              entry
              [ins/inspector hiccup]]]])
         [s/h1
          {:style
           {:margin 0
            :padding 40
            :font-size "2em"
            :color  (::c/namespace theme)}}
          [s/span
           {:on-click
            (fn [e]
              (.stopPropagation e)
              (when-let [el (.-current ref)]
                (.scrollIntoView ^js el)))
            :style
            {:cursor :pointer
             :color  (::c/tag theme)}} "# "]
          label])]]]]))

(defn render-docs [value]
  (let [has-label?  (string? (first value))
        has-article? (map? (second value))]
    [ins/with-collection
     value
     (when has-label?
       [ins/toggle-bg [render-article value]])
     (map-indexed
      (fn [index value]
        ^{:key index}
        [render-docs value])
      (cond-> value
        has-label?   rest
        has-article? rest))]))

(defn get-docs-order
  ([value]
   (persistent! (get-docs-order (transient []) value)))
  ([out value]
   (cond
     (or (not (coll? value)) (map? value)) out

     (string? (first value))
     (get-docs-order (conj! out (first value)) (rest value))

     :else (reduce get-docs-order out value))))

(defn get-parents
  ([value]
   (persistent! (get-parents (transient {}) nil value)))
  ([out parent value]
   (cond
     (map? value)     out
     (string? value) (cond-> out parent (assoc! value parent))

     (string? (first value))
     (let [k (first value)]
       (-> out
           (get-parents parent k)
           (get-parents k (rest value))))

     :else
     (reduce #(get-parents %1 parent %2) out value))))

(defn index-docs [value]
  (let [tree  (:cljdoc.doc/tree value)
        order (get-docs-order tree)]
    {:order   (zipmap order (range))
     :last    (last order)
     :parents (get-parents tree)}))

(defn inspect-cljdoc* [value]
  (let [theme                  (theme/use-theme)
        padding                (* 3 (:padding theme))
        background             (ins/get-background)
        tree                   (:cljdoc.doc/tree value)
        [visible set-visible!] (react/useState nil)]
    [with-observer
     #(set-visible! (merge visible %))
     [s/div
      {:style
       {:font-size     (:font-size theme)
        :display       :flex
        :background    background
        :max-width     (+ 280 896 3)
        :border        [1 :solid (::c/border theme)]
        :border-radius (:border-radius theme)}}
      [s/nav
       {:style
        {:top         0
         :line-height "1.1rem"
         :box-sizing  :border-box
         :padding-top padding
         :position    :sticky
         :height      :fit-content
         :min-width   280
         :max-height  "calc(100vh - 146px)"
         :overflow    :auto}}
       [docs-nav tree visible]]
      [s/div
       {:style
        {:flex        1
         :overflow    :auto
         :border-left [1 :solid (::c/border theme)]}}
       [render-docs tree]]]]))

(defn inspect-cljdoc [value]
  [with-index (index-docs value) [inspect-cljdoc* value]])

(defn cljdoc? [value]
  (contains? value :cljdoc.doc/tree))

(def viewer
  {:predicate cljdoc?
   :component inspect-cljdoc
   :name      :portal.viewer/cljdoc})

(ns portal.ui.styled
  (:require [clojure.string :as str]))

(def selectors
  {:style             #(str "." %)
   :style/hover       #(str "." % ":hover")
   :style/focus       #(str "." % ":focus")
   :style/placeholder #(str "." % "::placeholder")})

(defonce cache (atom {}))

(defn- value->css [v]
  (cond
    (number? v)  (str v "px")
    (keyword? v) (name v)
    (vector? v)  (str/join " " (map value->css v))
    (list? v)    (str (first v)
                      "("
                      (str/join ", " (map value->css (rest v)))
                      ")")
    :else        v))

(def exclude? #{:opacity :z-index})

(defn style->css [style]
  (reduce-kv
   (fn [css k v]
     (str
      css
      (when (and k v)
        (str (value->css k) ":"
             (if (exclude? k)
               v
               (value->css v)) ";")))) "" style))

(defn- generate-class [selector style]
  (let [css (style->css style)]
    (when-not (empty? css)
      (let [k  (gensym)
            f  (get selectors selector)
            el (js/document.createElement "style")]
        (set! (.-innerHTML el)
              (str (f k) "{" css "}"))
        (.appendChild js/document.head el)
        (swap! cache assoc [selector style] k)
        k))))

(defn- get-class [selector style]
  (or (get @cache [selector style])
      (generate-class selector style)))

(defn attrs->css [attrs]
  (reduce
   (fn [attrs selector]
     (if-not (contains? attrs selector)
       attrs
       (let [style (get attrs selector)
             class (get-class selector style)]
         (-> attrs
             (dissoc selector)
             (update :class str " " class)))))
   attrs
   (keys selectors)))

(defn styled [component attrs & children]
  (into [component
         (if-not (map? attrs)
           attrs
           (attrs->css attrs))]
        children))

(def a      (partial styled :a))
(def button (partial styled :button))
(def div    (partial styled :div))
(def h1     (partial styled :h1))
(def h2     (partial styled :h2))
(def h3     (partial styled :h3))
(def h4     (partial styled :h4))
(def h5     (partial styled :h5))
(def h6     (partial styled :h6))
(def iframe (partial styled :iframe))
(def img    (partial styled :img))
(def input  (partial styled :input))
(def nav    (partial styled :nav))
(def option (partial styled :option))
(def section (partial styled :section))
(def select (partial styled :select))
(def span   (partial styled :span))
(def table  (partial styled :table))
(def tbody  (partial styled :tbody))
(def td     (partial styled :td))
(def th     (partial styled :th))
(def thead  (partial styled :thead))
(def tr     (partial styled :tr))

(defn map->css [m]
  (reduce-kv
   (fn [css k v]
     (str css
          (str/join " " (map name k))
          "{" (style->css v) "}\n"))
   ""
   m))

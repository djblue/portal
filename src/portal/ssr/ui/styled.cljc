(ns portal.ssr.ui.styled
  (:require [clojure.string :as str]))

(def selectors
  {:style             #(str "." %)
   :style/hover       #(str "." % ":hover")
   :style/focus       #(str "." % ":focus")
   :style/placeholder #(str "." % "::placeholder")
   :style/parent-hover #(str ".parent:hover:has(." % "):not(:has(.parent:hover)) > ." %)})

;;(defonce cache (atom {}))

(def ^:dynamic *cache* nil)

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
            ;; f  (get selectors selector)
            ;; el (js/document.createElement "style")
            ]
        ;; (set! (.-innerHTML el) (str (f k) "{" css "}"))
        ;; (.appendChild js/document.head el)
        (when *cache* (swap! *cache* assoc [selector style] k))
        k))))

(defn- get-class [selector style]
  (or (when *cache* (get @*cache* [selector style]))
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

(def a      :a)
(def button :button)
(def div    :div)
(def h1     :h1)
(def h2     :h2)
(def h3     :h3)
(def h4     :h4)
(def h5     :h5)
(def h6     :h6)
(def iframe :iframe)
(def img    :img)
(def input  :input)
(def nav    :nav)
(def option :option)
(def section :section)
(def select :select)
(def span   :span)
(def table  :table)
(def tbody  :tbody)
(def td     :td)
(def th     :th)
(def thead  :thead)
(def tr     :tr)

(defn map->css [m]
  (reduce-kv
   (fn [css k v]
     (str css
          (str/join " " (map name k))
          "{" (style->css v) "}\n"))
   ""
   m))
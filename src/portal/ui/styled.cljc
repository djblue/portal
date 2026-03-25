(ns portal.ui.styled
  (:require [clojure.string :as str]))

(def selectors
  {:style             #(str "." %)
   :style/hover       #(str "." % ":hover")
   :style/focus       #(str "." % ":focus")
   :style/placeholder #(str "." % "::placeholder")
   :style/parent-hover #(str ".parent:hover:has(." % "):not(:has(.parent:hover)) > ." %)})

(def ^:dynamic *cache* nil)
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
      #?(:clj (let [css (style->css style)]
                (when-not (empty? css)
                  (let [k  (gensym)]
                    (when *cache* (swap! *cache* assoc [selector style] k))
                    k)))
         :cljs
         (let [k  (gensym)
               f  (get selectors selector)
               el (js/document.createElement "style")]
           (set! (.-innerHTML el)
                 (str (f k) "{" css "}"))
           (.appendChild js/document.head el)
           (swap! cache assoc [selector style] k)
           k)))))

(defn- get-class [selector style]
  (or  (when *cache* (get @*cache* [selector style]))
       (get @cache [selector style])
       (generate-class selector style)))

(defn attrs->css [attrs]
  (reduce-kv
   (fn [attrs selector _]
     (if-not (contains? attrs selector)
       attrs
       (let [style (get attrs selector)
             class (get-class selector style)]
         (-> attrs
             (dissoc selector)
             (update :class str " " class)))))
   attrs
   selectors))

(defn styled [component attrs & children]
  (into [component
         (if-not (map? attrs)
           attrs
           (attrs->css attrs))]
        children))

(def a      #?(:clj :a        :cljs (partial styled :a)))
(def button #?(:clj :button   :cljs (partial styled :button)))
(def div    #?(:clj :div      :cljs (partial styled :div)))
(def h1     #?(:clj :h1       :cljs (partial styled :h1)))
(def h2     #?(:clj :h2       :cljs (partial styled :h2)))
(def h3     #?(:clj :h3       :cljs (partial styled :h3)))
(def h4     #?(:clj :h4       :cljs (partial styled :h4)))
(def h5     #?(:clj :h5       :cljs (partial styled :h5)))
(def h6     #?(:clj :h6       :cljs (partial styled :h6)))
(def iframe #?(:clj :iframe   :cljs (partial styled :iframe)))
(def img    #?(:clj :img      :cljs (partial styled :img)))
(def input  #?(:clj :input    :cljs (partial styled :input)))
(def nav    #?(:clj :nav      :cljs (partial styled :nav)))
(def option #?(:clj :option   :cljs (partial styled :option)))
(def section #?(:clj :section :cljs (partial styled :section)))
(def select #?(:clj :select   :cljs (partial styled :select)))
(def span   #?(:clj :span     :cljs (partial styled :span)))
(def table  #?(:clj :table    :cljs (partial styled :table)))
(def tbody  #?(:clj :tbody    :cljs (partial styled :tbody)))
(def td     #?(:clj :td       :cljs (partial styled :td)))
(def th     #?(:clj :th       :cljs (partial styled :th)))
(def thead  #?(:clj :thead    :cljs (partial styled :thead)))
(def tr     #?(:clj :tr       :cljs (partial styled :tr)))

(defn map->css [m]
  (reduce-kv
   (fn [css k v]
     (str css
          (str/join " " (map name k))
          "{" (style->css v) "}\n"))
   ""
   m))

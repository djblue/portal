(ns examples.data
  (:require #?(:clj [clojure.java.io :as io])
            #?(:org.babashka/nbb [clojure.core]
               :default [examples.hacker-news :as hn])
            #?(:lpy [portal.runtime]
               :default [clojure.pprint :as pp])
            [examples.macros :refer [read-file]]
            [portal.colors :as c]
            [portal.viewer :as v])
  #?(:clj  (:import [java.io ByteArrayOutputStream File]
                    [java.net URI]
                    [java.util Date]
                    [java.util UUID])
     :org.babashka/nbb (:import)
     :cljs (:import [goog.math Long])
     :cljr (:import [System DateTime Guid Uri]
                    [System.IO File])
     :lpy  (:import [math :as Math]
                    [datetime :as datetime])))

#?(:clj
   (defn slurp-bytes [x]
     (with-open [out (ByteArrayOutputStream.)]
       (io/copy (io/input-stream x) out)
       (.toByteArray out))))

(def platform-data
  #?(:clj
     {::ratio 22/7
      ::long 4611681620380904123
      ::class File
      ::file (io/file "deps.edn")
      ::directory  (io/file ".")
      ::uri (URI. "https://github.com/djblue/portal")
      ::exception (try (/ 1 0) (catch Exception e e))
      ::io-exception (try (slurp "/hello") (catch Exception e e))
      ::user-exception (Exception. "hi")
      ::ex-info (ex-info "My message" {:my :data})
      ::uuid (UUID/randomUUID)
      ::date (Date.)
      ::binary (slurp-bytes (io/resource "screenshot.png"))
      ::bigint 42N}
     :cljr
     {::ratio 22/7
      ::long 4611681620380904123
      ::class File
      ::uri (Uri. "https://github.com/djblue/portal")
      ::exception (try (/ 1 0) (catch Exception e e))
      ::io-exception (try (slurp "/hello" :enc "utf8") (catch Exception e e))
      ::user-exception (Exception. "hi")
      ::ex-info (ex-info "My message" {:my :data})
      ::uuid (Guid/NewGuid)
      ::date (DateTime/Now)
      ::binary (File/ReadAllBytes "resources/screenshot.png")
      ::bigint 42N}
     :org.babashka/nbb
     {::promise (js/Promise.resolve 123)
      ::url (js/URL. "https://github.com/djblue/portal")
      ::uuid (random-uuid)
      ::date (js/Date.)
      ::bigint (js/BigInt "42")
      ::js-array (clj->js [0 1 2 3 4])
      ::js-object (clj->js {:hello "world"})}
     :cljs
     {::long (.fromString Long "4611681620380904123")
      ::promise (js/Promise.resolve 123)
      ::url (js/URL. "https://github.com/djblue/portal")
      ::uuid (random-uuid)
      ::date (js/Date.)
      ::bigint (js/BigInt "42")
      ::js-array (clj->js [0 1 2 3 4])
      ::js-object (clj->js {:hello "world"})}
     :lpy
     {::fractions 22/7
      ::promise (promise)
      ::uuid (random-uuid)
      ::date (datetime.datetime/now)
      ::py-list (python/list [0 1 2 3 4])
      ::py-dict (python/dict {"hello" "world"})}))

(def platform-collections
  #?(:bb nil
     :clj
     {:java.util/ArrayList (java.util.ArrayList.)
      :java.util/HashMap (java.util.HashMap.)
      :java.util/HashSet (java.util.HashSet.)
      :java.util/LinkedHashSet (java.util.LinkedHashSet.)
      :java.util/LinkedList (java.util.LinkedList.)
      :java.util/Stack (java.util.Stack.)
      :java.util/TreeSet (java.util.TreeSet.)
      :java.util/Vector (java.util.Vector.)}
     :default nil))

(def basic-data
  {::booleans #{true false}
   ::nil nil
   ::vector [1 2 4]
   ::character \A
   ::char-seq (seq "hi\n")
   "string-key" "string-value"
   ::list (list 1 2 3)
   ::set #{1 2 3}
   ::ns-symbol 'hello/world
   ::keyword :hello-world
   ::ns-keyword ::hello-world
   ::range (range 10)
   ::nested-vector [1 2 3 [4 5 6]]
   ::special-doubles [##NaN ##Inf ##-Inf]
   ::url-string "https://github.com/djblue/portal"})

(defrecord Point [x y])

(defn- gt [a b]
  (if-not (and (number? a) (number? b))
    -1
    (compare b a)))

(def clojure-data
  {::regex #"hello-world"
   ::sorted-map #?(:lpy nil
                   :default (sorted-map-by gt 3 "c" 2 "b" 1 "a"))
   ::sorted-set #?(:lpy nil
                   :default
                   (sorted-set-by gt 3 2 1))
   ::var #'portal.colors/themes
   ::with-meta (with-meta 'with-meta {:hello :world})
   ::tagged #?(:lpy nil
               :default (tagged-literal 'my/tag ["hello, world"]))
   {:example/settings 'complex-key} :hello-world
   ::atom (atom ::hello)
   ::function println
   (with-meta 'symbol-key-with-meta {:a :b}) ::value
   ;;TODO: fix me, causes infinite loop in demo
   ;;::range (range)
   ::record #?(:bb {:x 0 :y 0} :default (->Point 0 0))})

(def map-reflection-data
  (v/table
   '{asTransient
     [{:name asTransient
       :return-type
       clojure.lang.ITransientMap
       :declaring-class
       clojure.lang.PersistentArrayMap
       :parameter-types []
       :exception-types []
       :flags #{:public}}
      {:name asTransient
       :return-type
       clojure.lang.ITransientCollection
       :declaring-class
       clojure.lang.PersistentArrayMap
       :parameter-types []
       :exception-types []
       :flags #{:public :bridge :synthetic}}]
     assoc
     [{:name assoc
       :return-type
       clojure.lang.IPersistentMap
       :declaring-class
       clojure.lang.PersistentArrayMap
       :parameter-types [java.lang.Object java.lang.Object]
       :exception-types []
       :flags #{:public}}
      {:name assoc
       :return-type clojure.lang.Associative
       :declaring-class clojure.lang.PersistentArrayMap
       :parameter-types [java.lang.Object]
       :exception-types []
       :flags #{:public :bridge :synthetic}}]
     PersistentArrayMap
     [{:name clojure.lang.PersistentArrayMap
       :declaring-class clojure.lang.PersistentArrayMap
       :parameter-types [clojure.lang.IPersistentMap java.lang.Object<>]
       :exception-types []
       :flags #{:public}}
      {:name clojure.lang.PersistentArrayMap
       :declaring-class clojure.lang.PersistentArrayMap
       :parameter-types []
       :exception-types []
       :flags #{:protected}}
      {:name clojure.lang.PersistentArrayMap
       :declaring-class clojure.lang.PersistentArrayMap
       :parameter-types [java.lang.Object<>]
       :exception-types []
       :flags #{:public}}]
     count
     [{:name count
       :return-type int
       :declaring-class clojure.lang.PersistentArrayMap
       :parameter-types []
       :exception-types []
       :flags #{:public}}]
     create
     [{:name create
       :return-type clojure.lang.PersistentArrayMap
       :declaring-class clojure.lang.PersistentArrayMap
       :parameter-types
       [java.lang.Object<>]
       :exception-types []
       :flags #{:varargs}}
      {:name create
       :return-type clojure.lang.IPersistentMap
       :declaring-class clojure.lang.PersistentArrayMap
       :parameter-types [java.util.Map]
       :exception-types []
       :flags #{:public :static}}]
     empty
     [{:name empty
       :return-type clojure.lang.IPersistentMap
       :declaring-class clojure.lang.PersistentArrayMap
       :parameter-types []
       :exception-types []
       :flags #{:public}}
      {:name empty
       :return-type clojure.lang.IPersistentCollection
       :declaring-class clojure.lang.PersistentArrayMap
       :parameter-types []
       :exception-types []
       :flags #{:public :bridge :synthetic}}]
     meta
     [{:name meta
       :return-type clojure.lang.IPersistentMap
       :declaring-class clojure.lang.PersistentArrayMap
       :parameter-types []
       :exception-types []
       :flags #{:public}}]
     withMeta
     [{:name withMeta
       :return-type clojure.lang.IObj
       :declaring-class clojure.lang.PersistentArrayMap
       :parameter-types [clojure.lang.IPersistentMap]
       :exception-types []
       :flags #{:public :bridge :synthetic}}
      {:name withMeta
       :return-type clojure.lang.PersistentArrayMap
       :declaring-class clojure.lang.PersistentArrayMap
       :parameter-types [clojure.lang.IPersistentMap]
       :exception-types []
       :flags #{:public}}]}
   {:columns [:flags :parameter-types :return-type]}))

(def diff-data
  (v/diff
   [{::removed "value"
     ::same-key "same-value"
     ::change-type #{1 2}
     ::deep-change {:a 0}
     ::set #{0 1 2}
     ::vector [::a ::removed ::b]
     ::different-value ::old-key}
    {::added "value"
     ::same-key "same-value"
     ::change-type {:a :b :c :d}
     ::deep-change {:a 1}
     ::set #{1 2 3}
     ::vector [::a ::added ::b]
     ::different-value ::new-key}]))

(def diff-text-data
  #?(:lpy nil
     :default
     (v/diff-text
      [(with-out-str (pp/pprint (first diff-data)))
       (with-out-str (pp/pprint (second diff-data)))])))

(def string-data
  (v/for
   {::json "{\"hello\": 123}"
    ::edn "^{:portal.viewer/default :portal.viewer/tree} {:hello 123}"
    ::csv "a,b,c\n1,2,3\n4,5,6"
    ::markdown (read-file "README.md")
    ::jwt "eyJhbGciOiJIUzI1NiJ9.eyJuYW1lIjoiSm9lIENvZGVyIn0.5dlp7GmziL2QS06sZgK4mtaqv0_xX4oFUuTDh1zHK4U"}
    {::json ::v/json
     ::edn ::v/edn
     ::csv ::v/csv
     ::markdown ::v/markdown
     ::jwt ::v/jwt}))

(def hiccup
  (v/hiccup
   [:<>
    [:h1 "Hello, I'm hiccup"]
    [:a {:href "https://github.com/djblue/portal"} "djblue/portal"]
    [::v/inspector {:hello :world}]]))

(defn- sin [x]
  #?(:cljr (Math/Sin x) :default (Math/sin x)))

(def line-chart
  (v/vega-lite
   {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
    :data {:values
           (map #(-> {:time % :value (sin %)})
                (range 0 (* 2 3.14) 0.25))}
    :encoding {:x {:field "time" :type "quantitative"}
               :y {:field "value" :type "quantitative"}}
    :mark "line"}))

(def bar-chart
  (v/vega-lite
   {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
    :data
    {:values
     [{:a "A", :b 28}
      {:a "B", :b 55}
      {:a "C", :b 43}
      {:a "D", :b 91}
      {:a "E", :b 81}
      {:a "F", :b 53}]}
    :mark "bar"
    :encoding
    {:x
     {:field "a"
      :type "nominal"
      :axis {:labelAngle 0}}
     :y {:field "b", :type "quantitative"}}}))

(def pie-chart
  (v/vega-lite
   {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
    :description "A simple pie chart with labels."
    :data
    {:values
     [{:category "a", :value 4}
      {:category "b", :value 6}
      {:category "c", :value 10}
      {:category "d", :value 3}
      {:category "e", :value 7}
      {:category "f", :value 8}]}
    :encoding
    {:theta
     {:field "value"
      :type "quantitative"
      :stack true}
     :color
     {:field "category"
      :type "nominal"
      :legend nil}}
    :layer
    [{:mark {:type "arc", :outerRadius 80}}
     {:mark {:type "text", :radius 90}
      :encoding
      {:text
       {:field "category", :type "nominal"}}}]
    :view {:stroke nil}}))

(def tabular-data
  (map #(-> {:x % :y (sin %)})
       (range 0 (* 2 3.14) 0.25)))

(def numerical-collection
  {:x (range -3.12 3.14 0.1)
   :y (map #(+ % (/ (* -1 % % %) 6))
           (range -3.14 3.14 0.1))})

(def scatter-chart
  (v/vega-lite
   {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
    :description
    "A scatterplot showing horsepower and miles per gallons for various cars."
    :data {:url "https://vega.github.io/vega-lite/data/cars.json"}
    :mark "point"
    :encoding
    {:x
     {:field "Horsepower", :type "quantitative"}
     :y
     {:field "Miles_per_Gallon"
      :type "quantitative"}}}))

(def vconcat-chart
  (v/vega-lite
   {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
    :data {:url "https://vega.github.io/editor/data/weather.csv"},
    :transform [{:filter "datum.location === 'Seattle'"}],
    :vconcat
    [{:mark "bar",
      :encoding
      {:x {:timeUnit "month", :field "date", :type "ordinal"},
       :y
       {:aggregate "mean", :field "precipitation", :type "quantitative"}}}
     {:mark "point",
      :encoding
      {:x {:field "temp_min", :type "quantitative", :bin true},
       :y {:field "temp_max", :type "quantitative", :bin true},
       :size {:aggregate "count", :type "quantitative"}}}]}))

(def hconcat-chart
  (v/vega-lite
   {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
    :data {:url "https://vega.github.io/editor/data/weather.csv"},
    :transform [{:filter "datum.location === 'Seattle'"}],
    :hconcat
    [{:mark "bar",
      :encoding
      {:x {:timeUnit "month", :field "date", :type "ordinal"},
       :y {:aggregate "mean", :field "precipitation"}}}
     {:mark "point",
      :encoding
      {:x {:field "temp_min", :bin true},
       :y {:field "temp_max", :bin true},
       :size {:aggregate "count"}}}]}))

(def geographic-data
  (v/vega-lite
   {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
    :data {:url "https://vega.github.io/vega-lite/data/airports.csv"}
    :projection {:type "albersUsa"}
    :mark "circle"
    :encoding
    {:longitude
     {:field "longitude", :type "quantitative"}
     :latitude
     {:field "latitude", :type "quantitative"}
     :size {:value 10}}
    :config {:view {:stroke "transparent"}}}))

(def histogram-heatmap-2D
  (v/vega-lite
   {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
    :data {:url "https://vega.github.io/vega-lite/data/movies.json"}
    :transform
    [{:filter
      {:and
       [{:field "IMDB Rating", :valid true}
        {:field "Rotten Tomatoes Rating"
         :valid true}]}}]
    :mark "rect"
    :height 400
    :encoding
    {:x
     {:bin {:maxbins 60}
      :field "IMDB Rating"
      :type "quantitative"}
     :y
     {:bin {:maxbins 40}
      :field "Rotten Tomatoes Rating"
      :type "quantitative"}
     :color
     {:aggregate "count", :type "quantitative"}}
    :config {:view {:stroke "transparent"}}}))

(def force-directed
  (v/vega
   {:title "Force Directed Layout"
    :description
    "A node-link diagram with force-directed layout, depicting character co-occurrence in the novel Les Misérables."
    :scales
    [{:name "color"
      :type "ordinal"
      :domain {:data "node-data", :field "group"}
      :range {:scheme "category20c"}}]
    :marks
    [{:name "nodes"
      :type "symbol"
      :zindex 1
      :from {:data "node-data"}
      :on
      [{:trigger "fix"
        :modify "node"
        :values
        "fix === true ? {fx: node.x, fy: node.y} : {fx: fix[0], fy: fix[1]}"}
       {:trigger "!fix"
        :modify "node"
        :values "{fx: null, fy: null}"}]
      :encode
      {:enter
       {:fill {:scale "color", :field "group"}
        :stroke {:value "white"}}
       :update
       {:size
        {:signal "2 * nodeRadius * nodeRadius"}
        :cursor {:value "pointer"}}}
      :transform
      [{:type "force"
        :iterations 300
        :restart {:signal "restart"}
        :static {:signal "static"}
        :signal "force"
        :forces
        [{:force "center"
          :x {:signal "cx"}
          :y {:signal "cy"}}
         {:force "collide"
          :radius {:signal "nodeRadius"}}
         {:force "nbody"
          :strength {:signal "nodeCharge"}}
         {:force "link"
          :links "link-data"
          :distance {:signal "linkDistance"}}]}]}
     {:type "path"
      :from {:data "link-data"}
      :interactive false
      :encode
      {:update
       {:stroke {:value "#ccc"}
        :strokeWidth {:value 0.5}}}
      :transform
      [{:type "linkpath"
        :require {:signal "force"}
        :shape "line"
        :sourceX "datum.source.x"
        :sourceY "datum.source.y"
        :targetX "datum.target.x"
        :targetY "datum.target.y"}]}]
    :$schema
    "https://vega.github.io/schema/vega/v5.json"
    :signals
    [{:name "cx", :update "width / 2"}
     {:name "cy", :update "height / 2"}
     {:name "nodeRadius"
      :value 8
      :bind
      {:input "range", :min 1, :max 50, :step 1}}
     {:name "nodeCharge"
      :value -30
      :bind
      {:input "range"
       :min -100
       :max 10
       :step 1}}
     {:name "linkDistance"
      :value 30
      :bind
      {:input "range", :min 5, :max 100, :step 1}}
     {:name "static"
      :value true
      :bind {:input "checkbox"}}
     {:description
      "State variable for active node fix status."
      :name "fix"
      :value false
      :on
      [{:events
        "symbol:mouseout[!event.buttons], window:mouseup"
        :update "false"}
       {:events "symbol:mouseover"
        :update "fix || true"}
       {:events
        "[symbol:mousedown, window:mouseup] > window:mousemove!"
        :update "xy()"
        :force true}]}
     {:description
      "Graph node most recently interacted with."
      :name "node"
      :value nil
      :on
      [{:events "symbol:mouseover"
        :update "fix === true ? item() : node"}]}
     {:description
      "Flag to restart Force simulation upon data changes."
      :name "restart"
      :value false
      :on
      [{:events {:signal "fix"}
        :update "fix && fix.length"}]}]
    :data
    [{:name "node-data"
      :url "https://vega.github.io/vega-lite/data/miserables.json"
      :format {:type "json", :property "nodes"}}
     {:name "link-data"
      :url "https://vega.github.io/vega-lite/data/miserables.json"
      :format {:type "json", :property "links"}}]}))

(def radial-tree
  (v/vega
   {:title "Radial Tree"
    :description
    "An example of a radial layout for a node-link diagram of hierarchical data."
    :scales
    [{:name "color"
      :type "linear"
      :range {:scheme "magma"}
      :domain {:data "tree", :field "depth"}
      :zero true}]
    :marks
    [{:type "path"
      :from {:data "links"}
      :encode
      {:update
       {:x {:signal "originX"}
        :y {:signal "originY"}
        :path {:field "path"}
        :stroke {:value "#ccc"}}}}
     {:type "symbol"
      :from {:data "tree"}
      :encode
      {:enter
       {:size {:value 100}
        :stroke {:value "#fff"}}
       :update
       {:x {:field "x"}
        :y {:field "y"}
        :fill {:scale "color", :field "depth"}}}}
     {:type "text"
      :from {:data "tree"}
      :encode
      {:enter
       {:text {:field "name"}
        :fontSize {:value 9}
        :fill {:value "#fff"}
        :baseline {:value "middle"}}
       :update
       {:x {:field "x"}
        :y {:field "y"}
        :dx
        {:signal "(datum.leftside ? -1 : 1) * 6"}
        :angle
        {:signal
         "datum.leftside ? datum.angle - 180 : datum.angle"}
        :align
        {:signal
         "datum.leftside ? 'right' : 'left'"}
        :opacity {:signal "labels ? 1 : 0"}}}}]
    :$schema
    "https://vega.github.io/schema/vega/v5.json"
    :signals
    [{:name "labels"
      :value true
      :bind {:input "checkbox"}}
     {:name "radius"
      :value 280
      :bind {:input "range", :min 20, :max 600}}
     {:name "extent"
      :value 360
      :bind
      {:input "range", :min 0, :max 360, :step 1}}
     {:name "rotate"
      :value 0
      :bind
      {:input "range", :min 0, :max 360, :step 1}}
     {:name "layout"
      :value "tidy"
      :bind
      {:input "radio"
       :options ["tidy" "cluster"]}}
     {:name "links"
      :value "line"
      :bind
      {:input "select"
       :options
       ["line" "curve" "diagonal" "orthogonal"]}}
     {:name "originX", :update "width / 2"}
     {:name "originY", :update "height / 2"}]
    :data
    [{:name "tree"
      :url "https://vega.github.io/vega-lite/data/flare.json"
      :transform
      [{:type "stratify"
        :key "id"
        :parentKey "parent"}
       {:type "tree"
        :method {:signal "layout"}
        :size [1 {:signal "radius"}]
        :as ["alpha" "radius" "depth" "children"]}
       {:type "formula"
        :expr
        "(rotate + extent * datum.alpha + 270) % 360"
        :as "angle"}
       {:type "formula"
        :expr "PI * datum.angle / 180"
        :as "radians"}
       {:type "formula"
        :expr "inrange(datum.angle, [90, 270])"
        :as "leftside"}
       {:type "formula"
        :expr
        "originX + datum.radius * cos(datum.radians)"
        :as "x"}
       {:type "formula"
        :expr
        "originY + datum.radius * sin(datum.radians)"
        :as "y"}]}
     {:name "links"
      :source "tree"
      :transform
      [{:type "treelinks"}
       {:type "linkpath"
        :shape {:signal "links"}
        :orient "radial"
        :sourceX "source.radians"
        :sourceY "source.radius"
        :targetX "target.radians"
        :targetY "target.radius"}]}]}))

(def sunburst
  (v/vega
   {:title "Sunburst"
    :description
    "An example of a space-fulling radial layout for hierarchical data."
    :scales
    [{:name "color"
      :type "ordinal"
      :domain {:data "tree", :field "depth"}
      :range {:scheme "tableau20"}}]
    :marks
    [{:type "arc"
      :from {:data "tree"}
      :encode
      {:enter
       {:x {:signal "width / 2"}
        :y {:signal "height / 2"}
        :fill {:scale "color", :field "depth"}
        :tooltip
        {:signal
         "datum.name + (datum.size ? ', ' + datum.size + ' bytes' : '')"}}
       :update
       {:startAngle {:field "a0"}
        :endAngle {:field "a1"}
        :innerRadius {:field "r0"}
        :outerRadius {:field "r1"}
        :stroke {:value "white"}
        :strokeWidth {:value 0.5}
        :zindex {:value 0}}
       :hover
       {:stroke {:value "red"}
        :strokeWidth {:value 2}
        :zindex {:value 1}}}}]
    :$schema
    "https://vega.github.io/schema/vega/v5.json"
    :data
    [{:name "tree"
      :url "https://vega.github.io/vega-lite/data/flare.json"
      :transform
      [{:type "stratify"
        :key "id"
        :parentKey "parent"}
       {:type "partition"
        :field "size"
        :sort {:field "value"}
        :size
        [{:signal "2 * PI"}
         {:signal "width / 2"}]
        :as
        ["a0"
         "r0"
         "a1"
         "r1"
         "depth"
         "children"]}]}]}))

(def log-data
  (mapv
   #(-> %
        (v/log)
        (v/for {:time ::v/date-time}))
   [{:ns      'user
     :time    #inst "2006-03-24T14:49:31+00:00"
     :level   :info
     :column  1
     :line    1
     :result  :hello/clj
     :runtime :clj
     :form    :hello/clj}
    {:ns      'user
     :time    #inst "2009-02-03T00:25:17-06:00"
     :level   :debug
     :column  1
     :line    2
     :result  :hello/cljr
     :runtime :cljr
     :form    :hello/cljr}
    {:ns      'cljs.user
     :time    #inst "2011-06-02T19:45:57-04:00"
     :level   :warn
     :column  1
     :line    3
     :result  :hello/cljs
     :runtime :cljs
     :form    :hello/cljs}
    {:ns      'tasks
     :time    #inst "2019-08-09T14:51:42+02:00"
     :level   :error
     :column  1
     :line    4
     :result  :hello/bb
     :runtime :bb
     :form    :hello/bb}
    {:ns      'portal.api
     :time    #inst "2020-06-02T18:43:08-07:00"
     :level   :info
     :column  1
     :line    5
     :result  :hello/portal
     :runtime :portal
     :form    :hello/portal}
    {:ns      'nbb.core
     :time    #inst "2021-07-27T15:38:51+02:00"
     :level   :debug
     :column  1
     :line    6
     :result  :hello/nbb
     :runtime :nbb
     :form    :hello/nbb}
    {:ns      'joyride.core
     :time    #inst "2022-04-24T00:24:46+02:00"
     :level   :error
     :column  1
     :line    7
     :result  :hello/joyride
     :runtime :joyride
     :form    :hello/joyride}]))

(defn- profile-ms [v]
  (v/for v
    {:min ::v/duration-ns,
     :mean ::v/duration-ns,
     :p75 ::v/duration-ns,
     :p99 ::v/duration-ns,
     :p25 ::v/duration-ns,
     :p90 ::v/duration-ns,
     :max ::v/duration-ns,
     :mad ::v/duration-ns,
     :loc ::v/source-location,
     :p50 ::v/duration-ns,
     :sum ::v/duration-ns,
     :p95 ::v/duration-ns}))

(def profile-data
  (v/table
   {:get-x
    (profile-ms
     {:min 500379167,
      :mean 5.0368755E8,
      :p75 5.05059583E8,
      :mad-sum 8368282.0,
      :p99 5.0514774364E8,
      :n 5,
      :p25 5.02811792E8,
      :p90 5.051146834E8,
      :var 3.5110970180224E12,
      :max 505151417,
      :mad 1673656.4,
      :loc
      {:ns 'user,
       :line 16,
       :column 6,
       :file "/Users/chris/repos/portal/examples/tufte/src/user.clj"},
      :last 505151417,
      :p50 5.05035791E8,
      :sum 2518437750,
      :p95 5.051330502E8,
      :var-sum 1.7555485090112E13}),
    :get-y
    (profile-ms
     {:min 352537834,
      :mean 6.41027925E8,
      :p75 9.79101417E8,
      :mad-sum 1.388414716E9,
      :p99 9.964393760400001E8,
      :n 5,
      :p25 4.0308575E8,
      :p90 9.899376414000001E8,
      :var 8.1823301849941472E16,
      :max 997161791,
      :mad 2.776829432E8,
      :loc
      {:ns 'user,
       :line 17,
       :column 6,
       :file "/Users/chris/repos/portal/examples/tufte/src/user.clj"},
      :last 352537834,
      :p50 4.73252833E8,
      :sum 3205139625,
      :p95 9.935497162E8,
      :var-sum 4.0911650924970739E17})}
   {:columns [:n :min :max :mean :sum :loc]}))

(def table-data
  {::coll-of-maps
   (v/table log-data {:columns [:level :ns :result :runtime :time]})
   ::nested-maps
   (v/table
    {:ClojureScript {:nFiles 75 :blank 969 :comment 66 :code 7889}
     :ClojureC {:nFiles 27 :blank 337 :comment 3 :code 2785}
     :Clojure {:nFiles 41 :blank 288 :comment 11 :code 1909}
     :SUM {:blank 1594 :comment 80 :code 12583 :nFiles 143}}
    {:columns [:nFiles :blank :comment :code]})
   ::coll-of-vectors
   (v/table
    [[1 0 0 0 0]
     [0 1 0 0 0]
     [0 0 1 0 0]
     [0 0 0 1 0]
     [0 0 0 0 1]]
    {:columns [:a :b :c :d :e]})
   ::multi-map map-reflection-data})

(def test-report
  (v/test-report
   [{:type :begin-test-ns
     :ns   (if *ns* *ns* 'examples.data)}
    {:type :begin-test-var
     :var  #'test-report}
    {:type     :pass,
     :expected '(= 0 0),
     :actual   '(= 0 0),
     :message  nil}
    {:file     "NO_SOURCE_FILE",
     :type     :fail,
     :line     8,
     :expected '(= 0 (inc 0)),
     :actual   '(not (= 0 1)),
     :message  nil}
    {:type :end-test-var
     :var  #'test-report}
    {:type :end-test-ns
     :ns   (if *ns* *ns* 'examples.data)}]))

(def prepl-data
  (v/prepl
   [{:val
     "[1;34m=>[m [1;32mclojure[m [1m-M:cljfmt check dev src test extension-intellij/src/main/clojure[m\n",
     :tag :out}
    {:val "[1;34m->[m [1;33m6.872 seconds[m \n\n", :tag :out}
    {:val
     "[1;34m=>[m [1;32mclojure[m [1m-M:kondo --lint dev src test extension-intellij/src/main/clojure[m\n",
     :tag :out}
    {:val "[1;34m->[m [1;33m8.537 seconds[m \n\n", :tag :out}
    {:val "[1;34m=>[m [1;32mclojure[m [1m-M:cider:check[m\n",
     :tag :out}
    {:val "[1;34m->[m [1;33m3.77 seconds[m \n\n", :tag :out}
    {:val
     "[1;34m=>[m [1;32m./gradlew[m [1m--warning-mode all checkClojure[m\n",
     :tag :out}
    {:val "[1;34m->[m [1;33m1.477 seconds[m \n\n", :tag :out}
    {:val
     "[1;34m=>[m [1;32mclojure[m [1m-Sdeps {:deps #:org.clojure{clojurescript #:mvn{:version \"1.10.773\"}}} -M:test -m cljs.main --output-dir target/cljs-output-1.10.773 --target node --output-to target/test.1.10.773.js --compile portal.test-runner[m\n",
     :tag :out}
    {:val "[1;34m->[m [1;33m5.470 seconds[m \n\n", :tag :out}
    {:val "[1;34m=>[m [1;32mnode[m [1mtarget/test.1.10.773.js[m\n",
     :tag :out}
    {:val "[1;34m->[m [1;33m0.540 seconds[m \n\n", :tag :out}
    {:val
     "[1;34m=>[m [1;32mclojure[m [1m-Sdeps {:deps #:org.clojure{clojurescript #:mvn{:version \"1.10.844\"}}} -M:test -m cljs.main --output-dir target/cljs-output-1.10.844 --target node --output-to target/test.1.10.844.js --compile portal.test-runner[m\n",
     :tag :out}
    {:val "[1;34m->[m [1;33m6.328 seconds[m \n\n", :tag :out}
    {:val "[1;34m=>[m [1;32mnode[m [1mtarget/test.1.10.844.js[m\n",
     :tag :out}
    {:val "[1;34m->[m [1;33m0.307 seconds[m \n\n", :tag :out}
    {:val
     "[1;34m=>[m [1;32mplanck[m [1m-c src:test -m portal.test-planck[m\n",
     :tag :out}
    {:val "[1;34m->[m [1;33m2.39 seconds[m \n\n", :tag :out}
    {:val
     "[1;34m=>[m [1;32mclojure[m [1m-M:cljs:shadow -m shadow.cljs.devtools.cli release client[m\n",
     :tag :out}
    {:val "[1;34m->[m [1;33m38.140 seconds[m \n\n", :tag :out}
    {:val
     "[1;34m=>[m [1;32mclojure[m [1m-M:test -m portal.test-runner[m\n",
     :tag :out}
    {:val "[1;34m->[m [1;33m4.777 seconds[m \n\n", :tag :out}
    {:val "[1;34m=>[m [1;32mbb[m [1m-m portal.test-runner[m\n",
     :tag :out}
    {:val "[1;34m->[m [1;33m1.408 seconds[m \n\n", :tag :out}]))

(def exception-data
  (v/ex
   '{:runtime :clj
     :cause "My message",
     :via
     [{:type clojure.lang.ExceptionInfo,
       :at [clojure.lang.AFn applyToHelper "AFn.java" 156],
       :message "My message",
       :data {:my :data}}],
     :trace
     [[clojure.lang.AFn applyToHelper "AFn.java" 156]
      [clojure.lang.AFn applyTo "AFn.java" 144]
      [clojure.lang.Compiler$InvokeExpr eval "Compiler.java" 3706]
      [clojure.lang.Compiler$MapExpr eval "Compiler.java" 3058]
      [clojure.lang.Compiler$DefExpr eval "Compiler.java" 457]
      [clojure.lang.Compiler eval "Compiler.java" 7186]
      [clojure.lang.Compiler load "Compiler.java" 7640]
      [clojure.lang.RT loadResourceScript "RT.java" 381]
      [clojure.lang.RT loadResourceScript "RT.java" 372]
      [clojure.lang.RT load "RT.java" 459]
      [clojure.lang.RT load "RT.java" 424]
      [clojure.core$load$fn__6856 invoke "core.clj" 6115]
      [clojure.core$load invokeStatic "core.clj" 6114]
      [clojure.core$load doInvoke "core.clj" 6098]
      [clojure.lang.RestFn invoke "RestFn.java" 408]
      [clojure.core$load_one invokeStatic "core.clj" 5897]
      [clojure.core$load_one invoke "core.clj" 5892]
      [clojure.core$load_lib$fn__6796 invoke "core.clj" 5937]
      [clojure.core$load_lib invokeStatic "core.clj" 5936]
      [clojure.core$load_lib doInvoke "core.clj" 5917]
      [clojure.lang.RestFn applyTo "RestFn.java" 142]
      [clojure.core$apply invokeStatic "core.clj" 669]
      [clojure.core$load_libs invokeStatic "core.clj" 5974]
      [clojure.core$load_libs doInvoke "core.clj" 5958]
      [clojure.lang.RestFn applyTo "RestFn.java" 137]
      [clojure.core$apply invokeStatic "core.clj" 669]
      [clojure.core$require invokeStatic "core.clj" 5996]
      [clojure.core$require doInvoke "core.clj" 5996]
      [clojure.lang.RestFn invoke "RestFn.java" 408]
      [user$eval42500 invokeStatic "NO_SOURCE_FILE" 90]
      [user$eval42500 invoke "NO_SOURCE_FILE" 90]
      [clojure.lang.Compiler eval "Compiler.java" 7181]
      [clojure.lang.Compiler eval "Compiler.java" 7136]
      [clojure.core$eval invokeStatic "core.clj" 3202]
      [clojure.core$eval invoke "core.clj" 3198]
      [clojure.main$repl$read_eval_print__9110$fn__9113
       invoke
       "main.clj"
       437]
      [clojure.main$repl$read_eval_print__9110 invoke "main.clj" 437]
      [clojure.main$repl$fn__9119 invoke "main.clj" 458]
      [clojure.main$repl invokeStatic "main.clj" 458]
      [clojure.main$repl doInvoke "main.clj" 368]
      [clojure.lang.RestFn invoke "RestFn.java" 805]
      [companion.jvm$evaluate_clj$fn__40139 invoke "NO_SOURCE_FILE" 115]
      [clojure.core$with_redefs_fn invokeStatic "core.clj" 7516]
      [clojure.core$with_redefs_fn invoke "core.clj" 7500]
      [companion.jvm$evaluate_clj invokeStatic "NO_SOURCE_FILE" 112]
      [companion.jvm$evaluate_clj invoke "NO_SOURCE_FILE" 102]
      [companion.jvm$eval37720$fn__37721 invoke "jvm.clj" 248]
      [clojure.lang.MultiFn invoke "MultiFn.java" 229]
      [companion.jvm$evaluate invokeStatic "jvm.clj" 424]
      [companion.jvm$evaluate invoke "jvm.clj" 422]
      [user$eval42498 invokeStatic "NO_SOURCE_FILE" 1308]
      [user$eval42498 invoke "NO_SOURCE_FILE" 1308]
      [clojure.lang.Compiler eval "Compiler.java" 7181]
      [clojure.lang.Compiler eval "Compiler.java" 7136]
      [clojure.core$eval invokeStatic "core.clj" 3202]
      [clojure.core$eval invoke "core.clj" 3198]
      [shadow.cljs.devtools.server.socket_repl$repl$fn__17976
       invoke
       "socket_repl.clj"
       61]
      [clojure.main$repl$read_eval_print__9110$fn__9113
       invoke
       "main.clj"
       437]
      [clojure.main$repl$read_eval_print__9110 invoke "main.clj" 437]
      [clojure.main$repl$fn__9119 invoke "main.clj" 458]
      [clojure.main$repl invokeStatic "main.clj" 458]
      [clojure.main$repl doInvoke "main.clj" 368]
      [clojure.lang.RestFn invoke "RestFn.java" 805]
      [shadow.cljs.devtools.server.socket_repl$repl
       invokeStatic
       "socket_repl.clj"
       28]
      [shadow.cljs.devtools.server.socket_repl$repl
       invoke
       "socket_repl.clj"
       26]
      [shadow.cljs.devtools.server.socket_repl$connection_loop
       invokeStatic
       "socket_repl.clj"
       102]
      [shadow.cljs.devtools.server.socket_repl$connection_loop
       invoke
       "socket_repl.clj"
       72]
      [shadow.cljs.devtools.server.socket_repl$start$fn__17995$fn__17996$fn__17998
       invoke
       "socket_repl.clj"
       142]
      [clojure.lang.AFn run "AFn.java" 22]
      [java.lang.Thread run "Thread.java" 829]],
     :data {:my :data}}))

(def http-requests
  (mapv
   v/http
   [{:request-method :get
     :uri "https://djblue.github.io/portal/"}
    {:request-method :post
     :uri "https://djblue.github.io/portal/"}
    {:request-method :put
     :uri "https://djblue.github.io/portal/"}
    {:request-method :patch
     :uri "https://djblue.github.io/portal/"}
    {:request-method :delete
     :uri "https://djblue.github.io/portal/"}
    {:request-method :options
     :uri "https://djblue.github.io/portal/"}]))

(def http-responses
  (mapv
   v/http
   [{:status 100
     :headers {:content-type "text/html; charset=utf-8"}
     :body "<html><body>hi</body></html>"}
    {:status 200
     :headers {:content-type "text/html; charset=utf-8"}
     :body "<html><body>hi</body></html>"}
    {:status 300
     :headers {:content-type "text/html; charset=utf-8"}
     :body "<html><body>hi</body></html>"}
    {:status 400
     :headers {:content-type "text/html; charset=utf-8"}
     :body "<html><body>hi</body></html>"}
    {:status 500
     :headers {:content-type "text/html; charset=utf-8"}
     :body "<html><body>hi</body></html>"}]))

(def data-visualization
  {::vega
   {::force-directed force-directed
    ::radial-tree    radial-tree
    ::sunburst       sunburst}
   ::vega-lite
   {::line-chart           line-chart
    ::pie-chart            pie-chart
    ::bar-chart            bar-chart
    ::scatter-chart        scatter-chart
    ::vconcat-chart        vconcat-chart
    ::hconcat-chart        hconcat-chart
    ::histogram-heatmap-2D histogram-heatmap-2D
    ::geographic-data      geographic-data}
   ::portal-charts
   {::tabular-data         tabular-data
    ::numerical-collection numerical-collection}})

(def spec-data
  (v/default
   '{:clojure.spec.alpha/problems
     ({:path [:user/inst],
       :pred clojure.core/inst?,
       :val 1,
       :via [:user/coll :user/map :user/inst],
       :in [0 :user/inst]}
      {:path [:user/hello],
       :pred clojure.core/string?,
       :val :user/world,
       :via [:user/coll :user/map :user/hello],
       :in [0 :user/hello]}
      {:path [:user/test],
       :pred clojure.core/true?,
       :val nil,
       :via [:user/coll :user/map :user/test],
       :in [0 :user/test]}),
     :clojure.spec.alpha/spec :user/coll,
     :clojure.spec.alpha/value
     [{:user/inst 1, :user/hello :user/world, :user/test nil, :user/set d}]}
   :portal.viewer/spec))

(def data
  (merge
   {::platform-data      platform-data
    ::hacker-news        #?(:org.babashka/nbb nil :default hn/stories)
    ::spec-data          spec-data
    ::table-data         table-data
    ::diff               diff-data
    ::diff-text          diff-text-data
    ::basic-data         basic-data
    ::themes             c/themes
    ::clojure-data       clojure-data
    ::hiccup             hiccup
    ::data-visualization data-visualization
    ::string-data        string-data
    ::log-data           log-data
    ::profile-data       profile-data
    ::test-data          test-report
    ::prepl-data         prepl-data
    ::exception-data     exception-data
    ::http-requests      http-requests
    ::http-responses     http-responses}
   (when platform-data
     {::platform-collections platform-collections})))

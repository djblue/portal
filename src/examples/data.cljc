(ns examples.data
  (:require #?(:clj [clojure.java.io :as io])
            [examples.macros :refer [read-file]]
            [portal.colors :as c]
            [examples.hacker-news :as hn])
  #?(:clj (:import [java.io File ByteArrayOutputStream]
                   [java.net URI URL]
                   [java.util UUID])))

#?(:clj
   (defn slurp-bytes [x]
     (with-open [out (ByteArrayOutputStream.)]
       (io/copy (io/input-stream x) out)
       (.toByteArray out))))

(def platform-data
  #?(:clj {::ratio 22/7
           ::class File
           ::file (io/file "deps.edn")
           ::directory  (io/file ".")
           ::uri (URI. "https://github.com/djblue/portal")
           ::url (URL. "https://github.com/djblue/portal")
           ::exception (try (/ 1 0) (catch Exception e e))
           ::io-exception (try (slurp "/hello") (catch Exception e e))
           ::user-exception (Exception. "hi")
           ::uuid (UUID/randomUUID)
           ::date (java.util.Date.)
           ::binary (slurp-bytes (io/resource "screenshot.png"))}
     :cljs {::promise (js/Promise.resolve 123)
            ::url (js/URL. "https://github.com/djblue/portal")
            ::uuid (random-uuid)
            ::date (js/Date.)}))

(def basic-data
  {::booleans #{true false}
   ::nil nil
   ::vector [1 2 4]
   "string-key" "string-value"
   ::list (range 3)
   ::set #{1 2 3}
   ::ns-symbol 'hello/world
   ::keyword :hello-world
   ::ns-keyword ::hello-world
   ::range (range 10)
   ::nested-vector [1 2 3 [4 5 6]]
   ::url-string "https://github.com/djblue/portal"})

(def clojure-data
  {::regex #"hello-world"
   ::var #'portal.colors/themes
   ::with-meta (with-meta 'with-meta {:hello :world})
   {:example/settings 'complex-key} :hello-world
   ::atom (atom ::hello)
   ::function println
   ;;TODO: fix me, causes infininte loop in demo
   ;;::range (range)
   })

(def diff-data
  (with-meta
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
      ::different-value ::new-key}]
    {:portal.viewer/default :portal.viewer/diff}))

(def string-data
  {::json "{\"hello\": 123}"
   ::edn (pr-str {:hello 123})
   ::csv "a,b,c\n1,2,3\n4,5,6"
   ::markdown (read-file "README.md")})

(def hiccup
  (with-meta
    [:div
     [:h1 "Hello, I'm hiccup"]
     [:a {:href "https://github.com/djblue/portal"} "djblue/portal"]
     [:portal.viewer/inspector {:hello :world}]]
    {:portal.viewer/default :portal.viewer/hiccup}))

(def line-chart
  {:data {:values (map #(-> {:time % :value (Math/sin %)})
                       (range 0 (* 2 3.14) 0.25))}
   :encoding {:x {:field "time" :type "quantitative"}
              :y {:field "value" :type "quantitative"}}
   :mark "line"})

(def bar-chart
  {:data
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
    :y {:field "b", :type "quantitative"}}})

(def pie-chart
  {:description
   "A simple pie chart with labels."
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
   :view {:stroke nil}})

(def tabular-data
  (map #(-> {:x %
             :y (#?(:clj Math/sin
                    :cljs js/Math.sin) %)})
       (range 0 (* 2 3.14) 0.25)))

(def numerical-collection
  {:x (range -3.12 3.14 0.1)
   :y (map #(+ % (/ (* -1 % % %) 6))
           (range -3.14 3.14 0.1))})

(def scatter-chart
  {:$schema
   "https://vega.github.io/schema/vega-lite/v4.json"
   :description
   "A scatterplot showing horsepower and miles per gallons for various cars."
   :data {:url "https://vega.github.io/vega-lite/data/cars.json"}
   :mark "point"
   :encoding
   {:x
    {:field "Horsepower", :type "quantitative"}
    :y
    {:field "Miles_per_Gallon"
     :type "quantitative"}}})

(def geographic-data
  {:width "container"
   :height "container"
   :data {:url "https://vega.github.io/vega-lite/data/airports.csv"}
   :projection {:type "albersUsa"}
   :mark "circle"
   :encoding
   {:longitude
    {:field "longitude", :type "quantitative"}
    :latitude
    {:field "latitude", :type "quantitative"}
    :size {:value 10}}
   :config {:view {:stroke "transparent"}}})

(def histogram-heatmap-2D
  {:$schema
   "https://vega.github.io/schema/vega-lite/v4.json"
   :data {:url "https://vega.github.io/vega-lite/data/movies.json"}
   :transform
   [{:filter
     {:and
      [{:field "IMDB Rating", :valid true}
       {:field "Rotten Tomatoes Rating"
        :valid true}]}}]
   :mark "rect"
   :width "container"
   :height "container"
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
   :config {:view {:stroke "transparent"}}})

(def force-directed
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
     :format {:type "json", :property "links"}}]})

(def radial-tree
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
       :targetY "target.radius"}]}]})

(def sunburst
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
        "children"]}]}]})

(def data-visualization
  {::vega
   {::force-directed force-directed
    ::radial-tree radial-tree
    ::sunburst sunburst}
   ::vega-lite
   {::line-chart line-chart
    ::pie-chart pie-chart
    ::bar-chart bar-chart
    ::scatter-chart scatter-chart
    ::histogram-heatmap-2D histogram-heatmap-2D
    ::geographic-data geographic-data}
   ::portal-charts
   {::tabular-data tabular-data
    ::numerical-collection numerical-collection}})

(def data
  {::platform-data platform-data
   ::hacker-news hn/stories
   ::diff diff-data
   ::basic-data basic-data
   ::themes c/themes
   ::clojure-data clojure-data
   ::hiccup hiccup
   ::data-visualization data-visualization
   ::string-data string-data})

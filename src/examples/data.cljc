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
   ::range (range)})

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
     [:a {:href "https://github.com/djblue/portal"} "djblue/portal"]]
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

(def data-visualization
  {::vega-lite
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

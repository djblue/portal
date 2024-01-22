(ns notebook.scittle
  (:require [clojure.java.browse :as browse]
            [clojure.string :as str]
            [hiccup.page :as page]
            [portal.api :as p]
            [portal.viewer :as v]))

(def portal-dev (p/open {:mode :dev :launcher false}))

(defn portal-main-url [portal]
  (let [portal-url (p/url portal)
        [host query] (str/split portal-url #"\?")]
    (str host "/main.js?" query)))

(defn scittle-script [& cljs-forms]
  [:script {:type "application/x-scittle"}
   (->> cljs-forms
        (map pr-str)
        (str/join "\n"))])

(defn div-and-script [id widget]
  [[:div {:id id}]
   (scittle-script
    (list 'dom/render (list 'fn [] widget)
          (list '.getElementById 'js/document id)))])

(defn pr-str-with-meta [value]
  (binding [*print-meta* true]
    (pr-str value)))

(defn portal-widget [value]
  ['portal-viewer {:edn-str (pr-str-with-meta value)}])

(defn page [widgets]
  (page/html5
   [:head]
   (into
    [:body
     (page/include-js "https://unpkg.com/react@18/umd/react.production.min.js"
                      "https://unpkg.com/react-dom@18/umd/react-dom.production.min.js"
                      "https://scicloj.github.io/scittle/js/scittle.js"
                      "https://scicloj.github.io/scittle/js/scittle.reagent.js"
                      (portal-main-url portal-dev))
     (scittle-script '(ns main
                        (:require [reagent.core :as r]
                                  [reagent.dom :as dom]))
                     '(defn portal-viewer [{:keys [edn-str]}]
                        (let [embed (js/portal_api.embed)]
                          [:div
                           [:div
                            {:ref (fn [el]
                                    (.renderOutputItem embed
                                                       (clj->js {:mime "x-application/edn"
                                                                 :theme "portal.colors/nord-light"
                                                                 :text (fn [] edn-str)})
                                                       el))}]])))]
    (->> widgets
         (map-indexed (fn [i widget]
                        (div-and-script (str "widget" i)
                                        widget)))
         (apply concat)))))

(defn img [url]
  (v/hiccup
   [:img {:height 50 :width 50
          :src url}]))

(defn md [text]
  (v/hiccup [:portal.viewer/markdown text]))

(defn vega-lite-point-plot [data]
  (v/hiccup
   [:portal.viewer/vega-lite
    (-> {:data {:values data},
         :mark "point"
         :encoding
         {:size {:field "w" :type "quantitative"}
          :x {:field "x", :type "quantitative"},
          :y {:field "y", :type "quantitative"},
          :fill {:field "z", :type "nominal"}}})]))

(defn random-data [n]
  (->> (repeatedly n #(- (rand) 0.5))
       (reductions +)
       (map-indexed (fn [x y]
                      {:w (rand-int 9)
                       :z (rand-int 9)
                       :x x
                       :y y}))))

(defn random-vega-lite-plot [n]
  (-> n
      random-data
      vega-lite-point-plot))

(def example-values
  [(md "# embed portal in a scittle doc")
   (md "## plain data")
   {:x [1 3 4]}
   (md "## a vector of hiccups containing images")
   [(img "https://clojure.org/images/clojure-logo-120b.png")
    (img "https://raw.githubusercontent.com/djblue/portal/fbc54632adc06c6e94a3d059c858419f0063d1cf/resources/splash.svg")]
   (md "## a vega-lite plot")
   (random-vega-lite-plot 9)])

;; run

(comment
  (->> example-values
       (map portal-widget)
       page
       (spit "target/scittle.html"))
  (browse/browse-url "target/scittle.html"))
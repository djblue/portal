(ns portal.ui.sci.libs
  (:require ["react" :as react]
            ["react-dom" :as react-dom]
            ["vega" :as vega]
            ["vega-embed" :as vega-embed]
            ["vega-lite" :as vega-lite]
            cljs.reader
            portal.colors
            portal.ui.commands
            portal.ui.inspector
            [portal.ui.sci.import :as sci-import]
            portal.ui.state
            portal.ui.styled
            portal.ui.theme
            reagent.core
            reagent.dom
            [sci.core :as sci]))

(defn- import-npm [m]
  (if (fn? m)
    {'default m}
    (reduce-kv
     (fn [m k v]
       (assoc m (symbol k) v)) {}
     (js->clj m))))

(def namespaces
  (merge
   {"react"      (import-npm react)
    "react-dom"  (import-npm react-dom)
    "vega"       (import-npm vega)
    "vega-embed" (import-npm vega-embed)
    "vega-lite"  (import-npm vega-lite)}
   (sci-import/import-ns
    portal.colors
    portal.ui.commands
    portal.ui.inspector
    portal.ui.state
    portal.ui.styled
    portal.ui.theme
    reagent.core
    reagent.dom)
   (sci-import/import
    cljs.core/random-uuid
    cljs.core/tap>
    cljs.reader/read-string)))

(defn init [opts]
  (sci/init
   (merge {:namespaces namespaces
           :classes {'js js/window
                     :allow :all}
           :disable-arity-checks true}
          opts)))

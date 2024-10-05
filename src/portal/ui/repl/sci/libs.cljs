(ns ^:no-doc portal.ui.repl.sci.libs
  (:require ["react" :as react]
            ["react-dom" :as react-dom]
            ["react/jsx-runtime" :as jsx-runtime]
            ["vega" :as vega]
            ["vega-embed" :as vega-embed]
            ["vega-lite" :as vega-lite]
            cljs.reader
            clojure.zip
            goog.crypt.base64
            portal.colors
            portal.runtime.cson
            portal.ui.api
            portal.ui.app
            portal.ui.commands
            portal.ui.icons
            portal.ui.inspector
            portal.ui.options
            portal.ui.parsers
            [portal.ui.repl.sci.import :as sci-import]
            portal.ui.rpc
            portal.ui.select
            portal.ui.state
            portal.ui.styled
            portal.ui.theme
            portal.ui.viewer.bin
            portal.ui.viewer.charts
            portal.ui.viewer.code
            portal.ui.viewer.csv
            portal.ui.viewer.date-time
            portal.ui.viewer.diff
            portal.ui.viewer.edn
            portal.ui.viewer.exception
            portal.ui.viewer.hiccup
            portal.ui.viewer.html
            portal.ui.viewer.image
            portal.ui.viewer.json
            portal.ui.viewer.log
            portal.ui.viewer.markdown
            portal.ui.viewer.relative-time
            portal.ui.viewer.table
            portal.ui.viewer.text
            portal.ui.viewer.transit
            portal.ui.viewer.tree
            portal.ui.viewer.vega
            portal.ui.viewer.vega-lite
            portal.viewer
            [reagent.core :as r]
            [sci.configs.reagent.reagent :as reagent]
            [sci.core :as sci])
  (:import [goog.math Long]))

(def js-libs
  {"react"      react
   "react/jsx-runtime" jsx-runtime
   "react-dom"  react-dom
   "vega"       vega
   "vega-embed" vega-embed
   "vega-lite"  vega-lite})

(def namespaces
  (merge
   (sci-import/import-ns
    clojure.zip
    goog.crypt.base64
    goog.math
    portal.colors
    portal.runtime.cson
    portal.ui.api
    portal.ui.app
    portal.ui.commands
    portal.ui.icons
    portal.ui.inspector
    portal.ui.options
    portal.ui.parsers
    portal.ui.rpc
    portal.ui.select
    portal.ui.state
    portal.ui.styled
    portal.ui.theme
    portal.ui.viewer.bin
    portal.ui.viewer.charts
    portal.ui.viewer.code
    portal.ui.viewer.csv
    portal.ui.viewer.date-time
    portal.ui.viewer.diff
    portal.ui.viewer.edn
    portal.ui.viewer.exception
    portal.ui.viewer.hiccup
    portal.ui.viewer.html
    portal.ui.viewer.image
    portal.ui.viewer.json
    portal.ui.viewer.log
    portal.ui.viewer.markdown
    portal.ui.viewer.relative-time
    portal.ui.viewer.table
    portal.ui.viewer.text
    portal.ui.viewer.transit
    portal.ui.viewer.tree
    portal.ui.viewer.vega
    portal.ui.viewer.vega-lite
    portal.viewer)
   (sci-import/import
    cljs.core/random-uuid
    cljs.core/tap>
    cljs.reader/read-string)
   {'reagent.core
    (assoc reagent/reagent-namespace
           'adapt-react-class
           (sci/copy-var r/adapt-react-class reagent/rns)
           'render
           (sci/copy-var r/render reagent/rns))
    'reagent.ratom reagent/reagent-ratom-namespace
    'reagent.debug reagent/reagent-debug-namespace}))

(defn init [opts]
  (sci/init
   (merge {:namespaces namespaces
           :js-libs js-libs
           :classes {'js js/window
                     'Math js/Math
                     'goog.math.Long Long
                     :allow :all}
           :disable-arity-checks true}
          opts)))

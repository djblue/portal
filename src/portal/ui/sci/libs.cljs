(ns ^:no-doc portal.ui.sci.libs
  (:require ["react" :as react]
            ["react-dom" :as react-dom]
            ["vega" :as vega]
            ["vega-embed" :as vega-embed]
            ["vega-lite" :as vega-lite]
            cljs.reader
            goog.crypt.base64
            portal.colors
            portal.runtime.cson
            portal.ui.api
            portal.ui.app
            portal.ui.commands
            portal.ui.icons
            portal.ui.inspector
            portal.ui.rpc
            [portal.ui.sci.import :as sci-import]
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
            reagent.core
            reagent.dom
            [sci.core :as sci]))

(def js-libs
  {"react"      react
   "react-dom"  react-dom
   "vega"       vega
   "vega-embed" vega-embed
   "vega-lite"  vega-lite})

(def namespaces
  (merge
   (sci-import/import-ns
    goog.crypt.base64
    goog.math
    portal.colors
    portal.runtime.cson
    portal.ui.api
    portal.ui.app
    portal.ui.commands
    portal.ui.icons
    portal.ui.inspector
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
    reagent.core
    reagent.dom)
   (sci-import/import
    cljs.core/random-uuid
    cljs.core/tap>
    cljs.reader/read-string)))

(defn init [opts]
  (sci/init
   (merge {:namespaces namespaces
           :js-libs js-libs
           :classes {'js js/window
                     :allow :all}
           :disable-arity-checks true}
          opts)))

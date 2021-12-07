(ns portal.ui.sci.libs
  (:require cljs.reader
            portal.colors
            portal.ui.inspector
            [portal.ui.sci.import :as sci-import]
            portal.ui.state
            portal.ui.styled
            portal.ui.theme
            reagent.core
            reagent.dom
            [sci.core :as sci]))

(def namespaces
  (merge
   (sci-import/import-ns
    portal.colors
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

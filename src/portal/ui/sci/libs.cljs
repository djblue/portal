(ns portal.ui.sci.libs
  (:require [cljs.reader]
            [portal.ui.commands]
            [portal.ui.sci.import :as sci-import]
            [portal.ui.state]
            [reagent.core]
            [reagent.dom]
            [sci.core :as sci]))

(def namespaces
  #_:clj-kondo/ignore
  (sci-import/import
   cljs.core/random-uuid
   cljs.core/tap>

   cljs.reader/read-string

   portal.colors/themes
   portal.ui.inspector/inspector
   portal.ui.theme/use-theme

   portal.ui.styled/styled
   portal.ui.styled/a
   portal.ui.styled/h1
   portal.ui.styled/h2
   portal.ui.styled/h3
   portal.ui.styled/h4
   portal.ui.styled/h5
   portal.ui.styled/h6
   portal.ui.styled/table
   portal.ui.styled/table
   portal.ui.styled/tbody
   portal.ui.styled/thead
   portal.ui.styled/tr
   portal.ui.styled/th
   portal.ui.styled/td
   portal.ui.styled/div
   portal.ui.styled/span
   portal.ui.styled/input
   portal.ui.styled/button
   portal.ui.styled/img
   portal.ui.styled/iframe
   portal.ui.styled/select
   portal.ui.styled/option

   portal.ui.state/invoke

   reagent.core/as-element
   reagent.core/atom

   reagent.dom/render))

(defn init [opts]
  (sci/init
   (merge {:namespaces namespaces
           :classes {'js js/window
                     :allow :all}
           :disable-arity-checks true}
          opts)))

(ns portal.extensions.vs-code-notebook
  (:require [portal.runtime :as rt]
            [portal.runtime.edn :as edn]
            [portal.ui.embed :as embed]
            [portal.ui.inspector :as ins]
            [portal.ui.state :as state]
            [reagent.core :as r]
            [reagent.dom :as dom]))

(defonce context (atom nil))
(defonce component (r/atom embed/app))

(defn app [id value]
  [@component
   {:id id
    :value value
    :on-open
    (fn [value]
      (.postMessage
       ^js @context
       #js {:type "open-editor"
            :data (binding [*print-meta* true]
                    (pr-str value))}))}])

(defonce functional-compiler (r/create-compiler {:function-components true}))

(defn send! [msg]
  (when-let [f (get rt/ops (:op msg))]
    (js/Promise.
     (fn [resolve]
       (f msg resolve)))))

(defn render-output-item [data element]
  (let [value (try
                (edn/read-string (.text data))
                (catch :default e
                  (ins/error->data e)))]
    (dom/render [app (.-id data) value] element functional-compiler)))

(defn activate [ctx]
  (reset! context ctx)
  (reset! state/sender send!)
  #js {:renderOutputItem #(render-output-item %1 %2)})

(defn reload [] (reset! component embed/app))

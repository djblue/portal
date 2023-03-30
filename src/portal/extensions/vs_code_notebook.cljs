(ns portal.extensions.vs-code-notebook
  (:require [portal.async :as a]
            [portal.runtime :as rt]
            [portal.runtime.edn :as edn]
            [portal.ui.embed :as embed]
            [portal.ui.inspector :as ins]
            [portal.ui.rpc :as rpc]
            [portal.ui.state :as state]
            [reagent.core :as r]
            [reagent.dom :as dom]))

(defonce context (atom nil))
(defonce component (r/atom embed/app))

(defn inspect
  "Open a new portal window to inspect a particular value."
  {:command true}
  ([value]
   (inspect value))
  ([value _]
   (.postMessage
    ^js @context
    #js {:type "open-editor"
         :data (binding [*print-meta* true]
                 (pr-str value))})))

(rt/register! #'inspect {:name `portal.api/inspect})

(defn app [id value]
  [@component
   {:id id
    :value value
    :on-open #(rpc/call `portal.api/inspect % {:launcher :vs-code})}])

(defonce functional-compiler (r/create-compiler {:function-components true}))

(defn send! [msg]
  (when-let [f (get rt/ops (:op msg))]
    (js/Promise.
     (fn [resolve]
       (f msg resolve)))))

(defonce ^:private session (random-uuid))

(defn- ->value [data]
  (a/let [value (try
                  (edn/read-string (.text data))
                  (catch :default e
                    (ins/error->data e)))
          conn  (meta value)]
    (if-not (:portal.nrepl/eval conn)
      value
      (a/do
        (rpc/connect (assoc conn :session session))
        (reset! state/sender rpc/request)
        (apply rpc/call value)))))

(defn render-output-item [data element]
  (a/let [value (->value data)]
    (dom/unmount-component-at-node element)
    (dom/render [app (.-id data) value] element functional-compiler)))

(defn activate [ctx]
  (reset! context ctx)
  (reset! state/sender send!)
  #js {:renderOutputItem #(render-output-item %1 %2)})

(defn reload [] (reset! component embed/app))

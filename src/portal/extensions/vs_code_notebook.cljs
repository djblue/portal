(ns portal.extensions.vs-code-notebook
  (:require [clojure.string :as str]
            [portal.async :as a]
            [portal.runtime :as rt]
            [portal.runtime.edn :as edn]
            [portal.ui.cljs :as cljs]
            [portal.ui.embed :as embed]
            [portal.ui.inspector :as ins]
            [portal.ui.load :as load]
            [portal.ui.options :as opts]
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

(defn- app [id options value]
  [opts/with-options*
   options
   [@component
    {:id id
     :value value
     :on-open #(rpc/call `portal.api/inspect % {:launcher :vs-code})}]])

(defonce functional-compiler (r/create-compiler {:function-components true}))

(defn send! [msg]
  (when-let [f (get rt/ops (:op msg))]
    (js/Promise.
     (fn [resolve]
       (f msg resolve)))))

(defonce ^:private session (random-uuid))

(defn- ->text [^js data]
  (case (.-mime data)
    "application/vnd.code.notebook.error"
    (let [message (.-message (.json data))]
      (if (str/starts-with? message "\"^{")
        (edn/read-string message)
        (throw (ex-info
                message
                (js->clj data :keywordize-keys true)))))
    "x-application/edn"
    (.text data)))

(defn- ->value [data]
  (a/let [value (try
                  (edn/read-string (->text data))
                  (catch :default e
                    (ins/error->data e)))
          conn  (meta value)]
    (if-not (:portal.nrepl/eval conn)
      value
      (a/do
        (rpc/connect (assoc conn :session session))
        (reset! load/conn conn)
        (reset! state/sender rpc/request)
        (apply rpc/call value)))))

(defn- ->options [^js data]
  (when-let [theme (.-theme data)]
    {:portal.colors/theme (keyword theme)}))

(defn render-output-item [^js data element]
  (a/let [value (->value data)]
    (dom/unmount-component-at-node element)
    (dom/render [app (.-id data) (->options data) value] element functional-compiler)))

(defn activate [ctx]
  (cljs/init)
  (reset! context ctx)
  (reset! state/sender send!)
  #js {:renderOutputItem #(render-output-item %1 %2)
       :disposeOutputItem
       #(dom/unmount-component-at-node (.getElementById js/document %))})

(defn reload [] (reset! component embed/app))

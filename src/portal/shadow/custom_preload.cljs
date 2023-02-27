(ns portal.shadow.custom-preload
  (:require [portal.shadow.remote :as r]
            [portal.web :as p]))

(defn- submit [value]
       (p/submit value)
       (r/submit value))

(defn- error->data [ex]
       (merge
         (when-let [data (.-data ex)]
                   {:data data})
         {:runtime :portal
          :cause   (.-message ex)
          :via     [{:type    (symbol (.-name (type ex)))
                     :message (.-message ex)}]
          :stack   (.-stack ex)}))

(defn- async-submit [value]
       (cond
         (instance? js/Promise value)
         (-> value
             (.then async-submit)
             (.catch (fn [error]
                         (async-submit error)
                         (throw error))))

         (instance? js/Error value)
         (submit (error->data value))

         :else
         (submit value)))

(add-tap async-submit)

(defn- error-handler [event]
       (tap> (or (.-error event) (.-reason event))))

(.addEventListener js/window "error" error-handler)
(.addEventListener js/window "unhandledrejection" error-handler)

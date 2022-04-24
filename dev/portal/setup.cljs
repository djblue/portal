(ns portal.setup
  (:require [examples.data :refer [data]]
            [portal.client.web :as client]
            [portal.console :as log]
            [portal.ui.rpc :as rpc]
            [portal.ui.state :as state]
            [portal.web :as p]))

(def remote-submit (partial client/submit {:port js/window.location.port}))

(defonce tap-list (atom (list)))

(defn dashboard-submit [value]
  (swap! tap-list (fn [taps] (take 10 (conj taps value)))))
(defn ^:command clear-taps [] (swap! tap-list empty))
(defn ^:command clear-rpc [] (swap! rpc/log empty))

(defn submit [value]
  (remote-submit value)
  (dashboard-submit value))

(defn error->data [ex]
  (merge
   (when-let [data (.-data ex)]
     {:data data})
   {:cause (.-message ex)
    :via   [{:type    (symbol (.-name (type ex)))
             :message (.-message ex)}]
    :stack (.-stack ex)}))

(defn async-submit [value]
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

(add-tap #'async-submit)

(p/register! #'clear-taps)
(p/register! #'clear-rpc)

(defn dashboard []
  (with-meta
    [:div
     {:style
      {:display :flex
       :height  "100%"}}
     [:div
      {:style {:flex "1"}}
      [:h2 {:style {:margin-top 0}} "App State"]
      [:portal.viewer/inspector state/state]]
     [:div {:style {:width 20}}]
     [:div
      {:style {:flex           "1"
               :display        :flex
               :flex-direction :column}}
      [:h2 {:style {:margin-top 0}} "Tap List"]
      [:portal.viewer/inspector tap-list]
      [:h2 {:style {:margin-top 0}} "RPC Log"]
      [:portal.viewer/inspector rpc/log]]]
    {:portal.viewer/default :portal.viewer/hiccup}))

(p/set-defaults! {:mode :dev :value (dashboard)})

(.addEventListener js/window "error" #(tap> (.-error %)))

(comment
  (def portal (p/open))
  (def portal (p/open {:mode :dev :value (dashboard)}))
  (def portal (p/open {:mode :dev :value state/state}))

  (-> @portal)
  (tap> :hi)

  (add-tap #'p/submit)
  (tap> [{:hello :world :old-key 123} {:hello :youtube :new-key 123}])
  (doseq [i (range 100)] (tap> [::index i]))
  (p/clear)
  (p/close)

  (-> @portal)
  (tap> portal)
  (swap! portal * 1000)
  (reset! portal 1)

  (tap> (js/Error. "hi"))
  (tap> #js [1 2 3 4 5])
  (tap> (js/Promise.resolve 123))

  (tap> (with-meta (range) {:hello :world}))
  (tap> data)

  (do (log/trace ::trace)
      (log/debug ::debug)
      (log/info  ::info)
      (log/warn  ::warn)
      (log/error ::error)))

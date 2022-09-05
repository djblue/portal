(ns portal.setup
  (:require [examples.data :refer [data]]
            [portal.console :as log]
            [portal.shadow.remote :as remote]
            [portal.ui.inspector :as ins]
            [portal.ui.rpc :as rpc]
            [portal.ui.state :as state]
            [portal.web :as p]))

(defonce tap-list (atom (list)))

(defn dashboard-submit [value]
  (swap! tap-list (fn [taps] (take 10 (conj taps value)))))
(defn ^:command clear-taps [] (swap! tap-list empty))
(defn ^:command clear-rpc [] (swap! rpc/log empty))

(defn submit [value]
  (remote/submit value)
  (dashboard-submit value))

(defn async-submit [value]
  (cond
    (instance? js/Promise value)
    (-> value
        (.then async-submit)
        (.catch (fn [error]
                  (async-submit error)
                  (throw error))))

    (instance? js/Error value)
    (submit (ins/error->data value))

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

(defn- error-handler [event]
  #_(tap> (or (.-error event) (.-reason event)))
  (.error js/console event))

(.addEventListener js/window "error" error-handler)
(.addEventListener js/window "unhandledrejection" error-handler)

(comment
  (def portal (p/open))
  (def portal (p/open {:mode :dev :value (dashboard)}))
  (def portal (p/open {:mode :dev :value state/state}))

  (def ex (ex-info "error" {}))
  (js/Promise.reject ex)
  (js/setTimeout #(throw ex) 0)

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

(ns portal.setup
  (:require [examples.data :refer [data]]
            [portal.console :as log]
            [portal.shadow.remote :as remote]
            [portal.ui.api :as api]
            [portal.ui.commands :as commands]
            [portal.ui.inspector :as ins]
            [portal.ui.rpc :as rpc]
            [portal.ui.select :as select]
            [portal.ui.state :as state]
            [portal.web :as p]))

(defonce tap-list
  (atom (with-meta (list)
          {:portal.viewer/default :portal.viewer/inspector})))

(defn dashboard-submit [value]
  (swap! tap-list (fn [taps] (take 10 (conj taps value)))))
(defn ^:command clear-taps [] (swap! tap-list empty))
(defn ^:command clear-rpc [] (swap! rpc/log empty))

(defn submit [value]
  (comment (remote/submit value))
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

(defn section [title value]
  [title
   {:hiccup
    [:div
     {:style {:padding 40}}
     [:h2 {:style {:margin-top 0}} title]
     [:portal.viewer/inspector value]]}])

(defn dashboard []
  {:cljdoc.doc/tree
   [["Portal"
     (section "Taps" tap-list)
     (section "State" state/state)
     ["RPC"
      (section "Logs" rpc/log)
      (section "Errors" state/errors)]
     (section "Viewers" api/viewers)
     ["Commands"
      (section "UI" commands/registry)
      (section "Runtime" commands/runtime-registry)]
     (section "Selection Index" select/selection-index)]]})

(p/set-defaults!
 {:mode :dev
  :value (dashboard)
  :window-title "portal-ui-runtime"})

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

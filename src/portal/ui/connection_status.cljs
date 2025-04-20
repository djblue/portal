(ns ^:no-doc portal.ui.connection-status
  (:require [portal.async :as a]
            [portal.ui.react :as react]
            [portal.ui.state :as state]))

(defn- timeout [ms]
  (js/Promise.
   (fn [_resolve reject]
     (js/setTimeout
      #(reject (ex-info "Timeout reached" {:duration ms})) ms))))

(def ^:private poll-interval-ms 5000)

(def ^:private disconnect-notification
  {:type :error
   :icon :exclamation-triangle
   :message "Runtime disconnected"})

(defn- use-conn-poll []
  (let [state (state/use-state)]
    (react/use-effect
     #js [state]
     (let [last-poller (atom nil)
           poller (fn poller []
                    (a/try
                      (a/race (state/invoke 'portal.runtime/ping)
                              (timeout poll-interval-ms))
                      (state/dispatch! state state/dismiss disconnect-notification)
                      (catch :default _
                        (state/dispatch! state state/notify disconnect-notification))
                      (finally
                        (when @last-poller
                          (reset! last-poller (js/setTimeout poller poll-interval-ms))))))]
       (reset! last-poller (js/setTimeout poller 0))
       (fn []
         (js/clearTimeout @last-poller)
         (reset! last-poller nil))))))

(defn poller [] (use-conn-poll) nil)
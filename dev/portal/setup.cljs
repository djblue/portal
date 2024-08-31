(ns portal.setup
  (:require [clojure.datafy :refer [datafy]]
            [examples.data :refer [data]]
            [lambdaisland.dom-types]
            [portal.console :as log]
            [portal.runtime :as rt]
            [portal.shadow.remote :as remote]
            [portal.shortcuts :as shortcuts]
            [portal.ui.api :as api]
            [portal.ui.commands :as commands]
            [portal.ui.inspector :as ins]
            [portal.ui.repl.sci.eval :as sci]
            [portal.ui.rpc :as rpc]
            [portal.ui.select :as select]
            [portal.ui.state :as state]
            [portal.web :as p]))

(defonce tap-list
  (atom (with-meta (list)
          {:portal.viewer/default :portal.viewer/inspector})))

(defn dashboard-submit [value]
  (swap! tap-list (fn [taps]
                    (with-meta
                      (take 10 (conj taps value))
                      (meta taps)))))

(defn ^:command clear-taps
  "Clear tap list."
  []
  (swap! tap-list empty))

(defn ^:command clear-rpc
  "Clear rpc logs."
  []
  (swap! state/log empty))

(defn ^:command goto
  [v]
  (rpc/call 'portal.runtime.jvm.editor/goto-definition v))

(rt/register! #'goto {:name 'portal.runtime.jvm.editor/goto-definition})
(p/register! #'clear-taps)
(p/register! #'clear-rpc)

(defn submit [value]
  (let [value (datafy value)]
    (comment (remote/submit value))
    (dashboard-submit value)))

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

(defn section [title value]
  [title
   {:hiccup
    [:div
     {:style {:padding 40}}
     [:h2 {:style {:margin-top 0}} title]
     [:portal.viewer/inspector value]]}])

(defn dashboard []
  {:cljdoc.doc/tree
   [["Portal Client"
     (section "Taps" tap-list)
     (section "State" state/state)
     (section "SCI Context" sci/ctx)
     ["RPC"
      (section "Logs" state/log)]
     (section "Viewers" api/viewers)
     ["Shortcuts"
      (section "Key Log"  @#'shortcuts/log)
      (section "Key Map" @#'commands/client-keymap)]
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
  (def portal (p/inspect (dashboard) {:mode :dev}))
  (def portal (p/inspect state/state {:mode :dev}))

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

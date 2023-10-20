(ns portal.runtime.debug
  (:require [portal.api :as p]
            [portal.runtime :as rt]))

(defn- section [title value]
  [(name title)
   {:hiccup
    [:div
     {:style {:padding 40}}
     [:h2 {:style {:margin-top 0}}
      (if (string? title)
        title
        [:portal.viewer/inspector title])]
     [:portal.viewer/inspector value]]}])

(defn- dashboard [session]
  {:cljdoc.doc/tree
   [["Portal Server"
     ["Runtime"
      (section "Sessions" rt/sessions)
      (section "Connections" rt/connections)
      (section "Commands" @#'rt/registry)]
     ["Session State"
      (section "Info" (dissoc session :options :value-cache :watch-registry :selected))
      (section :options (:options session))
      (section :value-cache (:value-cache session))
      (section :watch-registry (:watch-registry session))]]]})

(defn open [session]
  (p/inspect
   (dashboard session)
   (-> (:options session)
       (dissoc :debug)
       (assoc :window-title "portal-debug-server"))))

(defn close [instance]
  (when instance (p/close instance)))
(ns portal.ui.core
  (:require [portal.colors :as c]
            [portal.ui.state :refer [state tap-state]]
            [portal.ui.rpc :as rpc]
            [portal.ui.app :refer [app]]
            [reagent.dom :as rdom]))

(defn render-app []
  (rdom/render [app]
               (.getElementById js/document "root")))

(defn on-back []
  (swap! state
         (fn [state]
           (if-let [previous-state (:portal/previous-state state)]
             (assoc previous-state :portal/next-state state)
             state))))

(defn on-forward []
  (swap! state
         (fn [state]
           (if-let [next-state (:portal/next-state state)]
             (assoc next-state :portal/previous-state state)
             state))))

(defn on-nav [send! target]
  (-> (send!
       {:op :portal.rpc/on-nav
        :args [(:coll target) (:k target) (:value target)]})
      (.then #(when-not (= (:value %) (:portal/value @state))
                (swap! state
                       (fn [state]
                         (assoc state
                                :portal/previous-state state
                                :portal/next-state nil
                                :search-text ""
                                :portal/value (:value %))))))))

(defn on-clear [send!]
  (->
   (send! {:op :portal.rpc/clear-values})
   (.then #(swap! state
                  (fn [state]
                    (-> state
                        (dissoc :portal/value)
                        (assoc
                         :portal/previous-state nil
                         :portal/next-state nil)))))))

(defn merge-state [new-state]
  (let [theme (get c/themes (::c/theme new-state ::c/nord))
        merged-state (swap! tap-state merge new-state theme)]
    (when (false? (:portal/open? merged-state))
      (js/window.close))
    merged-state))

(defn load-state [send!]
  (-> (send!
       {:op              :portal.rpc/load-state
        :portal/state-id (:portal/state-id @tap-state)})
      (.then merge-state)
      (.then #(:portal/complete? %))))

(def default-settings
  (merge
   {:font/family "monospace"
    :font-size "12pt"
    :limits/string-length 100
    :limits/max-depth 1
    :limits/max-length 1000
    :layout/direction :row
    :spacing/padding 8
    :border-radius "2px"}))

(defn get-actions [send!]
  {:portal/on-clear (partial on-clear send!)
   :portal/on-nav   (partial on-nav send!)
   :portal/on-back  (partial on-back send!)
   :portal/on-forward (partial on-forward send!)
   :portal/on-load  (partial load-state send!)})

(defn long-poll []
  (let [on-load (or (:portal/on-load @state)
                    #(js/Promise.resolve false))]
    (.then (on-load)
           (fn [complete?]
             (when-not complete? (long-poll))))))

(defn main!
  ([] (main! (get-actions rpc/send!)))
  ([settings]
   (swap! state merge default-settings settings)
   (long-poll)
   (render-app)))

(defn reload! [] (render-app))

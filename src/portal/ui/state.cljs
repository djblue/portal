(ns portal.ui.state
  (:require [portal.colors :as c]
            [reagent.core :as r]))

(defonce state     (r/atom nil))
(defonce tap-state (r/atom nil))

(defn back [state]
  (when-let [previous-state (:portal/previous-state state)]
    (assoc previous-state :portal/next-state state)))

(defn on-back [] (swap! state #(or (back %) %)))

(defn forward [state]
  (when-let [next-state (:portal/next-state state)]
    (assoc next-state :portal/previous-state state)))

(defn on-forward [] (swap! state #(or (forward %) %)))

(defn on-first []
  (swap! state
         (fn [state]
           (->> (iterate back state) (take-while some?) last))))

(defn on-last []
  (swap! state
         (fn [state]
           (->> (iterate forward state) (take-while some?) last))))

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
                                :portal/key (:k target)
                                :portal/value (:value %))))))))

(defn get-path [state]
  (->> state
       (iterate :portal/previous-state)
       (take-while some?)
       drop-last
       (map :portal/key)
       reverse
       (into [])))

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

(defn set-theme! [theme]
  (swap! tap-state assoc ::c/theme theme))

(defn merge-state [new-state]
  (let [merged-state (swap! tap-state merge new-state)]
    (when (false? (:portal/open? merged-state))
      (js/window.close))
    merged-state))

(defn load-state [send!]
  (-> (send!
       {:op              :portal.rpc/load-state
        :portal/state-id (:portal/state-id @tap-state)})
      (.then merge-state)
      (.then #(:portal/complete? %))))

(defn get-actions [send!]
  {:portal/on-clear (partial on-clear send!)
   :portal/on-first on-first
   :portal/on-last  on-last
   :portal/on-nav   (partial on-nav send!)
   :portal/on-back  on-back
   :portal/on-forward on-forward
   :portal/on-load  (partial load-state send!)})

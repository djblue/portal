(ns portal.ui.state
  (:require [portal.colors :as c]
            [reagent.core :as r]))

(defonce state     (r/atom nil))
(defonce tap-state (r/atom nil))
(defonce commands  (r/atom '()))

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

(defn push [{:portal/keys [key value f] :as entry}]
  (when (or (symbol? key) (keyword? key))
    (let [entry (dissoc entry :portal/value)]
      (swap! commands
             (fn [commands]
               (take 100 (conj (remove #{entry} commands) entry))))))
  (swap! state
         (fn [state]
           (assoc state
                  :portal/previous-state state
                  :portal/next-state nil
                  :search-text ""
                  :portal/key   key
                  :portal/f     f
                  :portal/value value))))

(defn on-nav [send! target]
  (-> (send!
       {:op :portal.rpc/on-nav
        :args [(:coll target) (:k target) (:value target)]})
      (.then #(when-not (= (:value %) (:portal/value @state))
                (push
                 {:portal/key (:k target)
                  :portal/value (:value %)})))))

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
  (swap! tap-state merge new-state))

(defn load-state [send!]
  (-> (send!
       {:op              :portal.rpc/load-state
        :portal/state-id (:portal/state-id @tap-state)})
      (.then merge-state)
      (.then #(:portal/complete? %))))

(defn invoke [send! f & args]
  (-> (send!
       {:op :portal.rpc/invoke :f f :args args})
      (.then #(:return %))))

(defn more [send! value]
  (when-let [f (-> value meta :portal.runtime/more)]
    (-> (invoke send! f)
        (.then
         (fn [more]
           (swap! (if (contains? @state :portal/value)
                    state
                    tap-state)
                  update :portal/value
                  (fn [current]
                    (with-meta
                      (concat current more)
                      (meta more)))))))))

(defn get-actions [send!]
  {:portal/on-clear (partial on-clear send!)
   :portal/on-first on-first
   :portal/on-last  on-last
   :portal/on-nav   (partial on-nav send!)
   :portal/on-back  on-back
   :portal/on-forward on-forward
   :portal/on-load  (partial load-state send!)
   :portal/on-more  (partial more send!)
   :portal/on-invoke (partial invoke send!)})

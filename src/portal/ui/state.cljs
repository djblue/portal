(ns portal.ui.state
  (:require [portal.colors :as c]
            [reagent.core :as r]
            [portal.async :as a]))

(defonce state     (r/atom nil))
(defonce tap-state (r/atom nil))

(defn notify-parent [event]
  (when js/parent
    (js/parent.postMessage (js/JSON.stringify (clj->js event)) "*")))

(defn dispatch [settings f & args]
  (let [state (::state settings)]
    (a/let [next-state (apply f (assoc @state :send! (:send! settings)) args)]
      (when next-state
        (reset! state (dissoc next-state :send!))))))

(defn send! [settings message] ((:send! settings) message))

(def no-history [::previous-commands])

(defn history-back [state]
  (when-let [previous-state (:portal/previous-state state)]
    (merge previous-state
           {:portal/next-state state}
           (select-keys state no-history))))

(defn history-first [state]
  (->> (iterate history-back state) (take-while some?) last))

(defn history-forward [state]
  (when-let [next-state (:portal/next-state state)]
    (merge next-state
           {:portal/previous-state state}
           (select-keys state no-history))))

(defn history-last [state]
  (->> (iterate history-forward state) (take-while some?) last))

(defn- push-command [state {:portal/keys [key] :as entry}]
  (if-not (or (symbol? key) (keyword? key))
    state
    (let [entry (dissoc entry :portal/value)]
      (assoc state
             ::previous-commands
             (take 100 (conj (remove #{entry} (::previous-commands state)) entry))))))

(defn history-push [state {:portal/keys [key value f] :as entry}]
  (assoc (push-command state entry)
         :portal/previous-state state
         :portal/next-state nil
         :search-text ""
         :portal/key   key
         :portal/f     f
         :portal/value value))

(defn nav [settings {:keys [coll k value]}]
  (-> (send!
       settings
       {:op :portal.rpc/on-nav :args [coll k value]})
      (.then #(when-not (= (:value %) (:portal/value settings))
                (history-push
                 settings
                 {:portal/key k
                  :portal/value (:value %)})))))

(defn get-path [state]
  (->> state
       (iterate :portal/previous-state)
       (take-while some?)
       drop-last
       (map :portal/key)
       reverse
       (into [])))

(defn clear [settings]
  (-> (send! settings {:op :portal.rpc/clear-values})
      (.then #(-> settings
                  (dissoc :portal/value)
                  (assoc
                   :search-text ""
                   :portal/previous-state nil
                   :portal/next-state nil)))))

(defn set-theme [color]
  (when-let [el (js/document.querySelector "meta[name=theme-color]")]
    (.setAttribute el "content" color)))

(defn set-theme! [theme]
  (swap! tap-state assoc ::c/theme theme)
  (let [color (get-in c/themes [theme ::c/background2])]
    (set-theme color)
    (notify-parent
     {:type :set-theme :color color})))

(defn- merge-state [new-state]
  (swap! tap-state merge new-state))

(defn- load-state [send!]
  (-> (send!
       {:op              :portal.rpc/load-state
        :portal/state-id (:portal/state-id @tap-state)})
      (.then merge-state)
      (.then #(:portal/complete? %))))

(defn long-poll [send!]
  (-> (load-state send!)
      (.then
       (fn [complete?]
         (when-not complete? (long-poll send!))))))

(defn invoke [settings f & args]
  (-> (send!
       settings
       {:op :portal.rpc/invoke :f f :args args})
      (.then #(:return %))))

(defn more [settings value]
  (let [state (::state settings)]
    (when-let [f (-> value meta :portal.runtime/more)]
      (-> (invoke settings f)
          (.then
           (fn [more]
             (swap! (if (contains? @state :portal/value)
                      state
                      tap-state)
                    update :portal/value
                    (fn [current]
                      (with-meta
                        (concat current more)
                        (meta more))))))))))

(defn get-value [settings]
  (:portal/value settings (:portal/value @tap-state)))

(defn get-history [settings]
  (concat
   (reverse
    (take-while some? (rest (iterate :portal/previous-state settings))))
   (take-while some? (iterate :portal/next-state settings))))

(defn get-settings []
  (merge (select-keys @tap-state [::c/theme])
         @state
         {::state state}))

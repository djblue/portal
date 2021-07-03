(ns portal.ui.state
  (:require ["react" :as react]
            [portal.colors :as c]
            [reagent.core :as r]
            [portal.async :as a]))

(defonce sender (atom nil))
(defonce state  (r/atom nil))

(defn notify-parent [event]
  (when js/parent
    (js/parent.postMessage (js/JSON.stringify (clj->js event)) "*")))

(defn dispatch! [state f & args]
  (a/let [next-state (apply f @state args)]
    (when next-state (reset! state next-state))))

(def ^:private state-context (react/createContext nil))

(defn use-state [] (react/useContext state-context))

(defn with-state [state & children]
  (into [:r> (.-Provider state-context) #js {:value state}] children))

(defn get-selected-context [state]
  (or (:selected state)
      (when (contains? state :portal/value)
        {:depth 1
         :path []
         :value (:portal/value state)})
      (when (contains? state :portal/tap-list)
        {:depth 1
         :path []
         :value (:portal/tap-list state)})))

(defn get-selected-value [state] (:value (get-selected-context state)))

(defn clear-selected [state] (dissoc state :selected))

(defn- send! [message] (@sender message))

(def no-history [::previous-commands :portal/tap-list ::c/theme])

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
         :selected nil
         :portal/key   key
         :portal/f     f
         :portal/value value))

(defn toggle-expand [state context]
  (let [expanded? (get state :expanded? #{})]
    (assoc state :expanded?
           (if (contains? expanded? context)
             (disj expanded? context)
             (conj expanded? context)))))

(defn focus-selected [state context]
  (history-push state {:portal/value (:value context)}))

(defn- get-prev-value [context]
  (let [{:keys [pairs key path]} context]
    (when-let [[key value]
               (some (fn [[prev curr _next]]
                       (when (= (first curr) key) prev))
                     (partition 3 1 (concat [nil] pairs [nil])))]
      (assoc context
             :key key
             :value value
             :path (conj (pop path) key)))))

(defn- get-prev-key [context]
  (let [{:keys [pairs value]} context]
    (when-let [[key]
               (some (fn [[prev curr _next]]
                       (when (= (first curr) value) prev))
                     (partition 3 1 (concat [nil] pairs [nil])))]
      (assoc context :value key))))

(defn- get-prev [context]
  (if (:key? context)
    (get-prev-key context)
    (get-prev-value context)))

(defn select-prev [state context]
  (if-let [next (get-prev context)]
    (assoc state :selected next)
    state))

(defn- get-next-key [context]
  (let [{:keys [pairs value]} context]
    (when-let [[key]
               (some (fn [[_prev curr next]]
                       (when (= (first curr) value) next))
                     (partition 3 1 (concat [nil] pairs [nil])))]
      (assoc context :value key))))

(defn- get-next-value [context]
  (let [{:keys [pairs key path]} context]
    (when-let [[key value]
               (some (fn [[_prev curr next]]
                       (when (= (first curr) key) next))
                     (partition 3 1 (concat [nil] pairs [nil])))]
      (assoc context
             :key key
             :value value
             :path (conj (pop path) key)))))

(defn- get-next [context]
  (if (:key? context)
    (get-next-key context)
    (get-next-value context)))

(defn select-next [state context]
  (if-let [next (get-next context)]
    (assoc state :selected next)
    state))

(defn get-parent [context]
  (let [{:keys [collection key path]} context]
    (cond
      (and (map? collection)
           (not (:key? context)))
      (assoc context
             :key nil
             :value key
             :key? true
             :path (pop path)))))

(defn select-parent [state context]
  (if-let [parent (get-parent context)]
    (assoc state :selected parent)
    state))

(defn get-child [context]
  (let [{:keys [collection value path]} context]
    (cond
      (and (map? collection)
           (:key? context))
      (-> context
          (dissoc :key?)
          (assoc
           :key value
           :value (get collection value)
           :path (conj path value))))))

(defn select-child [state context]
  (if-let [child (get-child context)]
    (assoc state :selected child)
    state))

(defn get-path [state]
  (when-let [{:keys [key? path value]} (get-selected-context state)]
    (if-not key? path (conj path value))))

(defn set-theme [color]
  (when-let [el (js/document.querySelector "meta[name=theme-color]")]
    (.setAttribute el "content" color)))

(defn set-theme! [state theme]
  (let [color (get-in c/themes [theme ::c/background2])]
    (set-theme color)
    (notify-parent
     {:type :set-theme :color color}))
  (assoc state ::c/theme theme))

(defn invoke [f & args]
  (-> (send! {:op :portal.rpc/invoke :f f :args args})
      (.then #(:return %))))

(defn- merge-state [new-state]
  (swap! state merge new-state))

(defn- load-state [send!]
  (-> (send!
       {:op              :portal.rpc/load-state
        :portal/state-id (:portal/state-id @state)})
      (.then merge-state)
      (.then #(:portal/complete? %))))

(defn long-poll [send!]
  (-> (load-state send!)
      (.then
       (fn [complete?]
         (when-not complete? (long-poll send!))))))

(defn clear [state]
  (a/do
    (invoke 'portal.runtime/clear-values)
    (-> state
        (dissoc :portal/value)
        (assoc
         :portal/tap-list '()
         :search-text ""
         :selected nil
         :portal/previous-state nil
         :portal/next-state nil))))

(defn- send-selected-value [_ _ state state']
  (when (or (not (:selected state))
            (not= (get-selected-value state)
                  (get-selected-value state')))
    (invoke 'portal.runtime/update-selected (get-selected-value state'))))

(add-watch state :selected #'send-selected-value)

(defn nav [state context]
  (let [{:keys [collection key value]} context
        key (when (or (map? collection) (vector? collection)) key)]
    (when collection
      (a/let [value (invoke 'clojure.datafy/nav collection key value)]
        (history-push state {:portal/value value})))))

(defn more [state]
  (let [k (if (contains? state :portal/value)
            :portal/value
            :portal/tap-list)]
    (if-let [f (-> state k meta :portal.runtime/more)]
      (a/let [more-values (invoke f)]
        (update state
                k
                (fn [current]
                  (with-meta
                    (concat current more-values)
                    (meta more-values)))))
      state)))

(defn get-value [state]
  (:portal/value state (:portal/tap-list state)))

(defn get-history [state]
  (concat
   (reverse
    (take-while some? (rest (iterate :portal/previous-state state))))
   (take-while some? (iterate :portal/next-state state))))

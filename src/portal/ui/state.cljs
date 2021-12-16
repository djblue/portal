(ns ^:no-doc portal.ui.state
  (:require ["react" :as react]
            [portal.async :as a]
            [portal.colors :as c]
            [portal.ui.select :as select]
            [reagent.core :as r]))

(defonce sender (atom nil))
(defonce state  (r/atom {}))
(defonce errors (atom nil))

(defn- get-parent []
  (cond
    (exists? js/parent)           js/parent
    (exists? js/acquireVsCodeApi) (js/acquireVsCodeApi)))

(defonce ^:private parent (get-parent))

(defn notify-parent [event]
  (let [message (js/JSON.stringify (clj->js event))]
    (when parent
      (.postMessage ^js parent message "*"))))

(defn dispatch! [state f & args]
  (a/let [next-state (apply f @state args)]
    (when next-state (reset! state next-state))))

(def ^:private state-context (react/createContext nil))

(defn use-state [] (react/useContext state-context))

(defn with-state [state & children]
  (into [:r> (.-Provider state-context) #js {:value state}] children))

(defn get-selected-context [state]
  (if-let [selected (:selected state)]
    (first selected)
    (when (contains? state :portal/value)
      {:depth 1
       :path []
       :value (:portal/value state)})))

(defn get-selected-value [state] (:value (get-selected-context state)))

(defn selected-values [state] (map :value (:selected state)))

(defn select-context
  ([state context]
   (select-context state context false))
  ([state context multi?]
   (if-not multi?
     (assoc state :selected [context])
     (update state :selected conj context))))

(defn deselect-context
  [state context multi?]
  (if-not multi?
    (dissoc state :selected)
    (update state :selected #(remove #{context} %))))

(defn selected [state context]
  (some (fn [[index context']]
          (when (= context' context)
            index))
        (map-indexed vector (:selected state))))

(defn get-location
  "Get a stable location for a given context."
  [context]
  (select-keys context [:value :path]))

(defn clear-selected [state] (dissoc state :selected))

(defn- send! [message] (@sender message))

(def no-history [::previous-commands ::c/theme])

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
  (-> (push-command state entry)
      (assoc
       :portal/previous-state state
       :portal/key   key
       :portal/f     f
       :portal/value value)
      (dissoc :portal/next-state :selected)))

(defn toggle-expand [state context]
  (let [location (get-location context)
        default  (get-in state [:default-expand location])]
    (update-in state [:expanded? location]
               (fn [value]
                 (not (if (nil? value) default value))))))

(defn focus-selected [state context]
  (history-push state {:portal/value (:value context)}))

(defn select-prev [state context]
  (if-let [prev (or (select/get-prev context)
                    (-> context
                        select/get-parent
                        select/get-prev
                        select/get-child))]
    (select-context state prev)
    state))

(defn select-next [state context]
  (if-let [next (or (select/get-next context)
                    (-> context
                        select/get-parent
                        select/get-next
                        select/get-child))]
    (select-context state next)
    state))

(defn select-parent [state context]
  (if-let [parent (or (select/get-left context)
                      (select/get-parent context))]
    (select-context state parent)
    state))

(defn select-child [state context]
  (if-let [child (or (select/get-right context)
                     (select/get-child context))]
    (select-context state child)
    state))

(defn get-path [state]
  (when-let [{:keys [key? path value]} (get-selected-context state)]
    (if-not key? path (conj path value))))

(defn set-theme [color]
  (when-let [el (js/document.querySelector "meta[name=theme-color]")]
    (.setAttribute el "content" color)))

(defn set-theme! [state theme] (assoc state ::c/theme theme))

(defn set-title [title]
  (set! (.-title js/document) title))

(defn set-title! [title]
  (set-title title)
  (notify-parent
   {:type :set-title :title title}))

(defn invoke [f & args]
  (-> (send! {:op :portal.rpc/invoke :f f :args args})
      (.then (fn [{:keys [return error]}]
               (when error
                 (reset! errors (take 5 (conj @errors error))))
               (if-not error
                 return
                 (throw (ex-info "invoke exception" (clj->js error))))))))

(defonce value-cache (r/atom {}))

(defn clear [state]
  (a/do
    (invoke 'portal.runtime/clear-values)
    (reset! value-cache (with-meta {} {:key (.now js/Date)}))
    (-> state
        (dissoc :portal/value
                :search-text
                :selected
                :selected-viewers)
        (assoc
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

(defn get-value [state default]
  (:portal/value state default))

(defn get-history [state]
  (concat
   (reverse
    (take-while some? (rest (iterate :portal/previous-state state))))
   (take-while some? (iterate :portal/next-state state))))

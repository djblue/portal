(ns portal.ui.state
  (:require ["react" :as react]
            [portal.async :as a]
            [portal.colors :as c]
            [portal.ui.select :as select]
            [reagent.core :as r]))

(defonce sender (atom nil))
(defonce state  (r/atom {}))

(defn notify-parent [event]
  (when (exists? js/parent)
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
         :value (:portal/value state)})))

(defn get-selected-value [state] (:value (get-selected-context state)))

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
  (assoc (push-command state entry)
         :portal/previous-state state
         :portal/next-state nil
         :selected nil
         :portal/key   key
         :portal/f     f
         :portal/value value))

(defn toggle-expand [state context]
  (let [expanded? (get state :expanded? #{})
        location  (get-location context)]
    (assoc state :expanded?
           (if (contains? expanded? location)
             (disj expanded? location)
             (conj expanded? location)))))

(defn focus-selected [state context]
  (history-push state {:portal/value (:value context)}))

(defn select-prev [state context]
  (if-let [next (select/get-prev context)]
    (assoc state :selected next)
    state))

(defn select-next [state context]
  (if-let [next (select/get-next context)]
    (assoc state :selected next)
    state))

(defn select-parent [state context]
  (if-let [parent (or (select/get-left context)
                      (select/get-parent context))]
    (assoc state :selected parent)
    state))

(defn select-child [state context]
  (if-let [child (or (select/get-right context)
                     (select/get-child context))]
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

(defn set-title [title]
  (set! (.-title js/document) title))

(defn set-title! [title]
  (set-title title)
  (notify-parent
   {:type :set-title :title title}))

(defn invoke [f & args]
  (-> (send! {:op :portal.rpc/invoke :f f :args args})
      (.then (fn [{:keys [return error]}]
               (when error (tap> error))
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

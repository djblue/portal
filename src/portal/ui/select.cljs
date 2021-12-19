(ns ^:no-doc portal.ui.select
  (:require ["react" :as react]))

(defonce ^:private selection-index (atom {}))
(defonce ^:private index-context (react/createContext []))

(defn with-position [position & children]
  (let [index (conj (react/useContext index-context) position)]
    (into [:r> (.-Provider index-context) #js {:value index}] children)))

(defn adjacent [f & args]
  (fn select
    ([context] (select @selection-index context))
    ([selection-index context]
     (when-let [index (get selection-index context)]
       (get selection-index
            (conj (pop index) (apply f (last index) args)))))))

(def get-prev   (adjacent update :row dec))
(def get-next   (adjacent update :row inc))
(def get-left   (adjacent update :column dec))
(def get-right  (adjacent update :column inc))

(def get-first  (adjacent assoc :row :first))
(def get-last   (adjacent assoc :row :last))

(defn get-child
  ([context] (get-child @selection-index context))
  ([selection-index context]
   (when-let [index (get selection-index context)]
     (get selection-index
          (conj index {:row 0 :column 0})))))

(defn get-parent
  ([context] (get-parent @selection-index context))
  ([selection-index context]
   (when-let [index (get selection-index context)]
     (get selection-index (pop index)))))

(defn- compute-relative-index [selection-index index context]
  (let [position-index  {index context context index}
        selection-index (into selection-index position-index)]
    (cond-> position-index
      (nil? (get-prev selection-index context))
      (assoc (conj (pop index)
                   (assoc (last index) :row :first))
             context)

      (nil? (get-next selection-index context))
      (assoc (conj (pop index)
                   (assoc (last index) :row :last))
             context))))

(defn use-register-context [context viewer]
  (let [index (react/useContext index-context)]
    (react/useEffect
     (fn []
       (let [updates (compute-relative-index @selection-index index context)]
         (swap! selection-index merge updates)
         (fn []
           (apply swap! selection-index dissoc (keys updates)))))
     #js [(hash index) (hash context) (hash viewer)])))

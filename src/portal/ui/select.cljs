(ns portal.ui.select
  (:require ["react" :as react]))

(defonce ^:private selection-index (atom {}))
(defonce ^:private index-context (react/createContext []))

(defn use-register-context [context viewer]
  (let [index (react/useContext index-context)]
    (react/useEffect
     (fn []
       (swap! selection-index assoc index context context index)
       (fn []
         (swap! selection-index dissoc index context)))
     #js [(hash index) (hash context) (hash viewer)])))

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

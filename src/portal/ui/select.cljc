(ns ^:no-doc portal.ui.select
  (:require [portal.ui.react :as react]))

(def ^:dynamic *selection-index* nil)
(defonce ^:no-doc selection-index (atom {}))
(defonce ^:private position-context (react/create-context []))

(defn- get-selection-index [] (or *selection-index* selection-index))

(defn with-position [position & children]
  (let [index (conj (react/use-context position-context) position)]
    (apply react/provider position-context index children)))

(defn get-root [] (::root @(get-selection-index)))

;; TODO: share this code with `portal.ui.state`

(defn- atom? [value]
  #?(:clj (instance? clojure.lang.Atom value)
     :cljs
     (and (satisfies? cljs.core/IDeref value)
          (not (instance? cljs.core/Var value)))))

(defn- parent-atom? [context]
  (some-> context :parent :value atom?))

(defn- get-location
  "Get a stable location for a given context."
  [context]
  (with-meta
    (if (parent-atom? context)
      {:value ::deref :stable-path (:stable-path context)}
      #?(:clj {:value (System/identityHashCode (:value context))
               :stable-path (:stable-path context)}
         :default {:value (:value context) :stable-path (:stable-path context)}))
    {:context context}))

(defn- get-position [selection-index context]
  (get selection-index (get-location context)))

(defn- adjacent [f & args]
  (fn select
    ([context] (select @(get-selection-index) context))
    ([selection-index context]
     (when-let [index (get-position selection-index context)]
       (loop [i 0 tail (last index)]
         (when-not (== i 25)
           (let [tail (apply f tail args)]
             (or (get selection-index
                      (conj (pop index) tail))
                 (recur (inc i) tail)))))))))

(def get-prev   (adjacent update :row dec))
(def get-next   (adjacent update :row inc))
(def get-left   (adjacent update :column dec))
(def get-right  (adjacent update :column inc))

(def get-first  (adjacent assoc :row :first))
(def get-last   (adjacent assoc :row :last))

(defn get-child
  ([context]
   (get-child @(get-selection-index) context nil))
  ([context column-context]
   (get-child @(get-selection-index) context column-context))
  ([selection-index context column-context]
   (when-let [index (get-position selection-index context)]
     (or (get selection-index
              (conj index (-> selection-index
                              (get-position column-context)
                              (last)
                              (assoc :row 0))))
         (get selection-index
              (conj index {:row 0 :column 0}))
         (get selection-index
              (conj index {:row :first :column 0}))))))

(defn get-parent
  ([context] (get-parent @(get-selection-index) context))
  ([selection-index context]
   (when-let [index (get-position selection-index context)]
     (get selection-index (pop index)))))

(defn- compute-relative-index [selection-index index context]
  (let [location        (get-location context)
        position-index  {index context location index}
        selection-index (into selection-index position-index)]
    (if-not (seq index)
      position-index
      (cond-> position-index

        (= (:depth context) 1)
        (assoc ::root context)

        (nil? (get-prev selection-index context))
        (assoc (conj (pop index)
                     (assoc (last index) :row :first))
               context)

        (nil? (get-next selection-index context))
        (assoc (conj (pop index)
                     (assoc (last index) :row :last))
               context)))))

(defn use-position []
  (react/use-context position-context))

(defn use-register-context [context viewer]
  (let [position (use-position)
        selection-index (get-selection-index)]
    (react/use-effect
     #?(:clj  [position context viewer]
        :cljs [(hash position) (hash context) (hash viewer)])
     (let [updates (compute-relative-index @selection-index position context)]
       (swap! selection-index merge updates)
       (fn []
         (apply swap! selection-index dissoc (keys updates)))))))

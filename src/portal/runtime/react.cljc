(ns portal.runtime.react
  "Server-side render for react/react style components."
  (:refer-clojure :exclude [random-uuid])
  (:require
   [clojure.string :as str]
   [portal.runtime.polyfill :refer [random-uuid]]
   [portal.ui.styled :as d]))

;; [x] component macro expansion
;; [x] basic local state
;; [x] effects mount/unmount
;; [ ] propogate state changes
;; [x] rendering lists
;; [x] match lists via :key
;; [ ] teardown unmatched key elements
;; [x] recursive un-mount?
;; [x] create-context / use-context
;; [ ] optimal re-render (granular updates / cache reuse)
;; [ ] improve list key re-render (how keep lazy?)
;; [ ] enumerate dom for fast insert / delete (patch updates)
;; [ ] serialize hiccup and ship to client
;; [ ] dispatch client events to server / complete ui loop

(def ^:private ^:dynamic *id* nil)
(def ^:private ^:dynamic *state* nil)
(def ^:private ^:dynamic *effects* nil)
(def ^:private ^:dynamic *memos* nil)
(def ^:private ^:dynamic *hook* nil)
(def ^:private ^:dynamic *element* nil)
(def ^:private ^:dynamic *context* nil)
(def ^:private ^:dynamic *context-set* nil)
(def ^:private ^:dynamic *context-used* nil)
(def ^:private ^:dynamic *component-state* nil)

;; (defn use-id [] *id*)

(defn use-effect
  ([f]
   (use-effect f nil))
  ([f deps]
   (when-not *hook*
     (throw (ex-info "Must be called during render." {:element *element*})))
   (when-not (fn? f)
     (throw (ex-info "Effect must be function." {:element *element*})))
   (vswap! *effects* conj {:fn f :deps deps})
   nil))

(defn use-memo
  [f deps]
  (when-not *hook*
    (throw (ex-info "Must be called during render." {:element *element*})))
  (when-not (fn? f)
    (throw (ex-info "Memo must be function." {:element *element*})))
  (let [hook-id (:memo (vswap! *hook* update :memo (fnil inc -1)))
        memo    (get @*memos* hook-id)]
    (if (and (some? memo)
             (= (:deps memo) deps))
      (:value memo)
      (let [value (f)]
        (vswap! *memos* assoc hook-id {:value value :deps deps})
        value))))

(defn use-state [init-value]
  (when-not *hook*
    (throw (ex-info "Must be called during render." {:element *element*})))
  (let [component-id *id*
        hook-id (:state (vswap! *hook* update :state (fnil inc -1)))
        state-id [component-id hook-id]
        state *state*]
    [(get *component-state* hook-id init-value)
     (fn set-state! [value-or-fn]
       (let [value  (get-in @state state-id init-value)
             value' (if-not (fn? value-or-fn)
                      value-or-fn
                      (value-or-fn value))]
         (swap! state assoc-in state-id value')
         value'))]))

(defn use-atom
  ([a]
   (use-atom a identity))
  ([a f]
   (use-atom a f true))
  ([a f active?]
   (let [[state set-state!] (use-state (f @a))]
     (use-effect
      (fn []
        (when active?
          (let [id (random-uuid)
                cache (atom state)
                watcher
                (fn watcher [_ _ state state']
                  (when-not (= state state')
                    (let [value (f state')]
                      (when-not (= @cache value)
                        (reset! cache value)
                        (set-state! (cond-> value (fn? value) constantly))))))]
            (watcher nil nil state @a)
            (add-watch a id watcher)
            (fn []
              (remove-watch a id)))))
      [a active?])
     state)))

(defn- context-provider-component [id {:keys [value]} & children]
  (swap! *context-set* assoc id value)
  (if (= 1 (count children))
    (first children)
    (into [:<>] children)))

(defn create-context [default-value]
  (let [id (gensym "context")]
    {:id id
     :default-value default-value
     :provider (partial context-provider-component id)}))

(defn- resolve-context-value [component-context context]
  (get component-context (:id context) (:default-value context)))

(defn use-context [context]
  (when-not *hook*
    (throw (ex-info "Must be called during render." {:element *element*})))
  (vswap! *context-used* conj context)
  (resolve-context-value *context* context))

(defn provider [context value & children]
  (into [(:provider context) {:value value}] children))

(declare render*)

(defn- element-key [element]
  (or (when (map? (second element)) (:key (second element)))
      (:key (meta element))))

(defn- render-list [vdom state context elements]
  (let [prev-vdom-index (:vdom-index vdom)
        next-vdom-index (volatile! {})
        vdom-children
        (for [element elements
              :let [k    (element-key element)
                    vdom (-> (some-> prev-vdom-index deref (get k))
                             (render* state context element)
                             (assoc :key k))]]
          (do
            (vswap! next-vdom-index assoc k vdom)
            vdom))]
    {:element elements
     :vdom-index next-vdom-index
     :vdom-children vdom-children
     :output (doall (map :output vdom-children))}))

(defn- render-component-post [output id]
  (when-not (or (nil? output)
                (string? output)
                (vector? output))
    (throw (ex-info "Component must return vector, string or nil"
                    {:element *element* :output output})))
  (cond-> output
    (vector? output)
    (vary-meta assoc ::id id)))

(defn- render-unmount* [vdom state]
  (when-let [component-id (:id vdom)]
    (swap! state dissoc component-id))
  (run! #(render-unmount* % state) (:vdom-children vdom))
  (doseq [{effect :fn} (:effects vdom) :when effect] (effect)))

(defn- render-unmount [vdom state element]
  (render-unmount* vdom state)
  {:element element :output (list)})

(defn- component-replaced? [vdom element]
  (and (some? vdom)
       (or (not (identical? (nth (:element vdom) 0) (nth element 0)))
           (not= (:key vdom) (element-key element)))))

(defn- run-effect [effect]
  (let [f ((:fn effect))]
    (if (fn? f)
      (assoc effect :fn f)
      (dissoc effect :fn))))

(defn- reconcile-effects [prev-effects current-effects]
  (if-not prev-effects
    (mapv run-effect current-effects)
    (mapv (fn [prev curr]
            (if-not (or (nil? (:deps prev))
                        (not= (:deps prev) (:deps curr)))
              prev
              (do
                (when-let [f (:fn prev)] (f))
                (run-effect curr))))
          prev-effects
          current-effects)))

(defn- render-component-cached [vdom state context _element]
  (let [child (-> (:vdom-children vdom)
                  (nth 0)
                  (render* state
                           (cond-> (merge context (:context-set vdom)))
                           (:component-output vdom)))]
    (cond-> vdom
      (not= (:output vdom) (:output child))
      (assoc :output (:output child) :vdom-children [child]))))

(defn- use-cached? [vdom component-state context element]
  (and (= (:element vdom) element)
       (= (:component-state vdom) component-state)
       (= (:resolved-context-values vdom)
          (mapv #(resolve-context-value context %) (:context-used vdom)))))

(defn- render-component [vdom state context element]
  (if (component-replaced? vdom element)
    (do
      (render-unmount* vdom state)
      (recur nil state context element))
    (let [id (or (:id vdom) (random-uuid))
          component-state (get @state id)]
      (if (use-cached? vdom component-state context element)
        (render-component-cached vdom state context element)
        (let [component    (nth element 0)
              effects      (volatile! [])
              context-set  (atom {})
              context-used (volatile! [])
              memos        (volatile! (:memos vdom []))
              output  (binding [*id*      id
                                *element*   component
                                *hook*    (volatile! {})
                                *state*   state
                                *memos*   memos
                                *effects* effects
                                *component-state* component-state
                                *context* context
                                *context-set* context-set
                                *context-used* context-used]
                        (try
                          (render-component-post
                           (let [cnt (count element)]
                             (case cnt
                               1 (component)
                               2 (component (nth element 1))
                               3 (component (nth element 1) (nth element 2))
                               4 (component (nth element 1) (nth element 2) (nth element 3))
                               (apply component (subvec element 1))))
                           id)
                          (catch #?(:clj Exception :cljs :default) e
                            #_(tap> (Throwable->map e))
                            [:pre (pr-str e)])))
              child-context (merge context @context-set)
              child   (-> (:vdom-children vdom)
                          (first)
                          (render* state child-context output))
              previous-calls (count (:effects vdom))
              current-calls (count @effects)]
          (when (and (not (zero? previous-calls))
                     (not= previous-calls current-calls))
            (throw (ex-info "Effects invoked different number of times."
                            {:element element
                             :previous-calls previous-calls
                             :current-calls current-calls})))
          {:id id
           :key (element-key element)
           :memos @memos
           :context-set @context-set
           :context-used @context-used
           :resolved-context-values (mapv #(resolve-context-value context %) @context-used)
           :effects (reconcile-effects (:effects vdom) @effects)
           :component-output output
           :component-state component-state
           :output (if (= (:output vdom) (:output child))
                     (:output vdom)
                     (:output child))
           :vdom-children [child]
           :element element})))))

(defn- hiccup-only-tree? [element]
  (or (nil? element)
      (string? element)
      (and (vector? element)
           (keyword? (nth element 0))
           (let [start (if (map? (nth element 1 nil)) 2 1)
                 cnt   (count element)]
             (loop [i start]
               (if (>= i cnt)
                 true
                 (if (hiccup-only-tree? (nth element i))
                   (recur (unchecked-inc i))
                   false)))))))

(defn- render-hiccup [vdom state context element]
  (if (and (:hiccup-only-tree? vdom)
           (= (:element vdom) element))
    vdom
    (let [n  (count element)
          id (or (when (and (> n 1) (map? (nth element 1)))
                   (-> element (nth 1) :id))
                 (:id vdom)
                 (random-uuid))
          tag           (nth element 0)
          has-attrs?    (map? (nth element 1 nil))
          attrs         (d/attrs->css (if has-attrs? (nth element 1) {}))
          child-start   (int (if has-attrs? 2 1))
          child-count   (- n child-start)
          vdom-children (let [prev (:vdom-children vdom)]
                          (loop [i 0 out (transient [])]
                            (if (>= i child-count)
                              (persistent! out)
                              (recur (unchecked-inc i)
                                     (conj! out
                                            (-> (get prev i)
                                                (render* state context (nth element (+ child-start i)))))))))
          output (with-meta
                   (into [tag attrs] (map :output) vdom-children)
                   (assoc (meta element) ::id id))]
      {:id id
       :output (if-not (= output (:output vdom))
                 output
                 (:output vdom))
       :element element
       :vdom-children vdom-children
       :hiccup-only-tree? (hiccup-only-tree? element)})))

(def ^:private re-escape #"[&<>\"]")

(defn- needs-escape? [^String s] (re-find re-escape s))

(defn- escape-html [s]
  (cond-> s
    (needs-escape? s)
    (str/escape {\& "&amp;" \< "&lt;" \> "&gt;" \" "&quot;"})))

(defn- render* [vdom state context element]
  (cond
    (nil? element)
    (render-unmount vdom state element)

    (string? element)
    (if (= (:element vdom) element)
      vdom
      {:element element
       :output (cond-> element
                 (needs-escape?  element)
                 (escape-html))})

    (or (list? element) (seq? element))
    (render-list vdom state context element)

    (and (vector? element) (fn? (nth element 0 nil)))
    (render-component vdom state context element)

    (and (vector? element) (keyword? (nth element 0 nil)))
    (render-hiccup vdom state context element)

    :else
    (if (= (:element vdom) element)
      vdom
      {:element element :output (str element)})))

(defn render
  ([element]
   (render {:state (atom {})} element))
  ([vdom element]
   (let [{:keys [output] :as vdom'} (render* vdom (:state vdom) nil element)]
     (with-meta output (assoc vdom' :state (:state vdom))))))
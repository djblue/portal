(ns portal.ssr.ui.react
  "Server-side render for react/react style components."
  (:refer-clojure :exclude [random-uuid])
  (:require [portal.ssr.ui.uuid :refer [random-uuid]]))

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

(def ^:dynamic *id* nil)
(def ^:dynamic *state* nil)
(def ^:dynamic *effects* nil)
(def ^:dynamic *hook* nil)
(def ^:dynamic *debug* {})
(def ^:dynamic *context* nil)
(def ^:dynamic *context-set* nil)
(def ^:dynamic *context-used* nil)
(def ^:dynamic *component-state* nil)

(defn use-id [] *id*)

(defn use-effect
  ([f]
   (use-effect f nil))
  ([f deps]
   (when-not *hook*
     (throw (ex-info "Must be called during render." *debug*)))
   (when-not (fn? f)
     (throw (ex-info "Effect must be function." *debug*)))
   (swap! *effects* conj {:fn f :deps deps})))

(defn use-state [init-value]
  (when-not *hook*
    (throw (ex-info "Must be called during render." *debug*)))
  (let [component-id *id*
        hook-id (:state (swap! *hook* update :state (fnil inc 0)))
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
    (throw (ex-info "Must be called during render." *debug*)))
  (swap! *context-used* conj context)
  (resolve-context-value *context* context))

(defn provider [context value & children]
  (into [(:provider context) {:value value}] children))

(declare render*)

(defn- render-list [vdom state context elements]
  (let [vdom-children
        (for [element elements
              :let [k (or (:key (meta element))
                          (:key (second element)))
                    vdom (some
                          (fn [vdom] (when (= k (:key vdom)) vdom))
                          (:vdom-children vdom))]]
          (assoc (render* vdom state context element) :key k))]
    {:element elements
     :vdom-children vdom-children
     :output (map :output vdom-children)}))

(defn- render-component-post [output id]
  (when-not (or (nil? output)
                (string? output)
                (vector? output))
    (throw (ex-info "Component must return vector or nil" *debug*)))
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
       (or (not (identical? (first (:element vdom)) (first element)))
           (not= (:key vdom)
                 (or (:key element)
                     (:key (meta element)))))))

(defn- render-component [vdom state context element]
  (if (component-replaced? vdom element)
    (do
      (render-unmount* vdom state)
      (recur nil state context element))
    (let [id (or (:id vdom) (random-uuid))
          effects (atom [])
          [component & args] element
          debug {:component (first element) :args args}
          context-set (atom {})
          context-used (atom [])
          component-state (get @state id)
          use-cached (and (= (:element vdom) element)
                          (= (:component-state vdom) component-state)
                          (= (:resolved-context-values vdom)
                             (mapv #(resolve-context-value context %) (:context-used vdom))))
          output  (if use-cached
                    (:component-output vdom)
                    (binding [*id*      id
                              *debug*   debug
                              *hook*    (atom {})
                              *state*   state
                              *effects* effects
                              *component-state* component-state
                              *context* context
                              *context-set* context-set
                              *context-used* context-used]
                      (render-component-post (apply component args) id)))
          child-context (if use-cached
                          (merge context (:context-set vdom))
                          (merge context @context-set))
          child   (-> (:vdom-children vdom)
                      (first)
                      (render* state child-context output))
          previous-calls (count (:effects vdom))
          current-calls (count @effects)]
      (when-not use-cached
        (when (and (not (zero? previous-calls))
                   (not= previous-calls current-calls))
          (throw (ex-info "Effects invoked different number of times."
                          (assoc debug
                                 :previous-calls previous-calls
                                 :current-calls current-calls)))))
      {:id id
       :key (or (:key element) (:key (meta element)))
       :context-set (if use-cached (:context-set vdom) @context-set)
       :context-used (if use-cached (:context-used vdom) @context-used)
       :resolved-context-values (if use-cached
                                  (:resolved-context-values vdom)
                                  (mapv #(resolve-context-value context %) @context-used))
       :effects (if use-cached
                  (:effects vdom)
                  (if-not (contains? vdom :effects)
                    (mapv
                     (fn [current-effect]
                       (let [f ((:fn current-effect))]
                         (if (fn? f)
                           (assoc current-effect :fn f)
                           (dissoc current-effect :fn))))
                     @effects)
                    (mapv
                     (fn [prev-effect current-effect]
                       (if-not (or (nil? (:deps prev-effect))
                                   (not= (:deps prev-effect)
                                         (:deps current-effect)))
                         prev-effect
                         (do
                           (when-let [effect (:fn prev-effect)] (effect))
                           (let [f ((:fn current-effect))]
                             (if (fn? f)
                               (assoc current-effect :fn f)
                               (dissoc current-effect :fn))))))
                     (:effects vdom)
                     @effects)))
       :component-output output
       :component-state (if use-cached (:component-state vdom) component-state)
       :output (if (= (:output vdom) (:output child))
                 (:output vdom)
                 (:output child))
       :vdom-children [child]
       :element element})))

(defn- render-hiccup [vdom state context element]
  (let [id (or (when (map? (second element))
                 (get-in element [1 :id]))
               (:id vdom)
               (random-uuid))
        [tag & args] element
        attrs (if-not (map? (first args))
                {}
                (first args))
        children (cond-> args (map? (first args)) (rest))
        vdom-children (vec
                       (map-indexed
                        (fn [index element]
                          (-> (:vdom-children vdom)
                              (get index)
                              (render* state context element)))
                        children))
        output (with-meta
                 (into [tag attrs] (map :output) vdom-children)
                 (assoc (meta element) ::id id))]
    {:id id
     :output (if-not (= output (:output vdom))
               output
               (:output vdom))
     :vdom-children vdom-children
     :element element}))

(defn- render* [vdom state context element]
  (cond
    ;;  (= (:element vdom) element)
    ;;  vdom

    (nil? element)
    (render-unmount vdom state element)

    (string? element)
    {:element element :output element}

    (or (list? element) (seq? element))
    (render-list vdom state context element)

    (and (vector? element) (fn? (first element)))
    (render-component vdom state context element)

    (and (vector? element) (keyword? (first element)))
    (render-hiccup vdom state context element)

    :else
    {:element element :output element}))

(defn render
  ([element]
   (render {:state (atom {})} element))
  ([vdom element]
   (let [{:keys [output] :as vdom'} (render* vdom (:state vdom) nil element)]
     (with-meta output (assoc vdom' :state (:state vdom))))))

;; (defn- diff-list [ops a b])

;; (defn- diff-attributes [ops a b] ops)

;; (defn- diff-children [ops a b] ops)

;; (defn- diff-node [ops a b]
;;   (conj ops {:remove a} {:append b}))

;; (defn diff
;;   ([a b]
;;    (diff [] a b))
;;   ([ops a b]
;;    (cond
;;      ;; elements type did not change
;;      (and (vector? a)
;;           (vector? b)
;;           (= (first a) (first b)))
;;      (let [[a-props & a-children] (rest a)
;;            [b-props & b-children] (rest b)]
;;        (-> ops
;;            (diff-attributes a-props b-props)
;;            (diff-children a-children b-children)))

;;      ;; diff lists based on children
;;      (and (list? a) (list? b))
;;      (diff-list ops a b)

;;      (not= a b)
;;      (diff-node ops a b)

;;      :else ops)))

;; (diff [:div {} "hello, "]
;;       [:div {} "hello, world"])
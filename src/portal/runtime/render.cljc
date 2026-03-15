(ns portal.runtime.render
  "Server-side render for react/react style components.")

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

(def ^:dynamic *state* nil)
(def ^:dynamic *effects* nil)
(def ^:dynamic *hook* nil)
(def ^:dynamic *debug* nil)
(def ^:dynamic *context* nil)

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
  (let [id (:state (swap! *hook* update :state (fnil inc 0)))
        state *state*
        current-value (get @state id init-value)]
    [current-value
     (fn [value-or-fn]
       (if-not (fn? value-or-fn)
         (swap! state assoc id value-or-fn)
         (swap! state assoc id (value-or-fn current-value))))]))

(defn use-atom [a]
  (let [[state set-state!] (use-state @a)]
    (use-effect
     (fn []
       (add-watch a ::use-atom
                  (fn watcher [_ _ state state']
                    (when (not= state state')
                      (set-state! state'))))
       #(remove-watch a ::use-atom))
     [a])
    state))

(defn- context-provider-component [id {:keys [value]} & children]
  (swap! *context* update id (fnil conj []) value)
  (if (= 1 (count children))
    (first children)
    (into [:<>] children)))

(defn create-context [default-value]
  (let [id (gensym "context")]
    {:id  id
     :default-value default-value
     :provider (partial context-provider-component id)}))

(defn use-context [context]
  (when-not *hook*
    (throw (ex-info "Must be called during render." *debug*)))
  (last (get @*context* (:id context) [(:default-value context)])))

(declare render*)

(defn- render-list [vdom elements]
  (let [context (:context vdom)
        vdom-children
        (for [element elements
              :let [k (or (:key (meta element))
                          (:key (second element)))
                    vdom (some
                          (fn [vdom] (= k (:key vdom)))
                          (:vdom-children vdom))]]
          (assoc (render* (assoc vdom :context context) element) :key k))]
    {:element elements
     :vdom-children vdom-children
     :output (map :output vdom-children)}))

(defn- render-component-post [output]
  (when-not (or (nil? output)
                (string? output)
                (vector? output))
    (throw (ex-info "Component must return vector or nil" *debug*)))
  output)

(defn- render-component [vdom element]
  (let [state   (:state vdom (atom {}))
        effects (atom [])
        [component & args] element
        context (atom (:context vdom))
        debug {:component (first element) :args args}
        output  (binding [*debug*   debug
                          *hook*    (atom {})
                          *state*   state
                          *effects* effects
                          *context* context]
                  (render-component-post (apply component args)))
        child   (-> (:vdom-children vdom)
                    (first)
                    (assoc :context @context)
                    (render* output))
        previous-calls (count (:effects vdom))
        current-calls (count @effects)]
    (when (and (not (zero? previous-calls))
               (not= previous-calls current-calls))
      (throw (ex-info "Effects invoked different number of times."
                      (assoc debug
                             :previous-calls previous-calls
                             :current-calls current-calls))))
    {:state state
     :effects (if-not (contains? vdom :effects)
                (mapv
                 (fn [current-effect]
                   (update current-effect :fn (fn [effect] (effect))))
                 @effects)
                (mapv
                 (fn [prev-effect current-effect]
                   (when (or (nil? (:deps prev-effect))
                             (not= (:deps prev-effect)
                                   (:deps current-effect)))
                     (when-let [effect (:fn prev-effect)] (effect))
                     (update current-effect :fn (fn [effect] (effect)))))
                 (:effects vdom)
                 @effects))
     :output (:output child)
     :vdom-children [child]
     :element element}))

(defn- render-hiccup [vdom element]
  (let [[tag & args] element
        attrs (if-not (map? (first args))
                {}
                (first args))
        children (cond-> args (map? (first args)) (rest))
        vdom-children (map-indexed
                       (fn [index element]
                         (-> (:vdom-children vdom)
                             (nth index)
                             (assoc :context (:context vdom))
                             (render* element)))
                       children)
        output (into [tag attrs] (map :output) vdom-children)]
    {:output output
     :vdom-children vdom-children
     :element element}))

(defn- render-unmount* [vdom]
  (run! render-unmount* (:vdom-children vdom))
  (doseq [{effect :fn} (:effects vdom)] (effect)))

(defn- render-unmount [vdom element]
  (render-unmount* vdom)
  {:element element :output (list)})

(defn- render* [vdom element]
  (cond
    ;;  (= (:element vdom) element)
    ;;  vdom

    (nil? element)
    (render-unmount vdom element)

    (string? element)
    {:element element :output element}

    (or (list? element) (seq? element))
    (render-list vdom element)

    (and (vector? element) (fn? (first element)))
    (render-component vdom element)

    (and (vector? element) (keyword? (first element)))
    (render-hiccup vdom element)

    :else
    {:element element :output element}))

(defn render
  ([element]
   (render nil element))
  ([vdom element]
   (let [{:keys [output] :as vdom} (render* vdom element)]
     (with-meta output vdom))))

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

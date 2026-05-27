(ns portal.ui.find
  (:require [portal.runtime.react :as react]
            [portal.ui.inspector :as ins]
            [portal.ui.lazy :as lazy]
            [portal.ui.state :as state]
            [portal.ui.theme :as theme]))

(defn find-location [state value predicate?]
  (->> [theme/with-theme
        :portal.colors/nord
        [theme/with-theme+
         {:max-depth 100
          ::lazy/default-take 1000
          ::ins/default-expand true}
         [state/with-state
          (atom @state)
          [ins/inspector value]]]]
       (react/render {:state (atom {::react/lazy true})})
       (meta)
       (tree-seq :element :vdom-children)
       (keep
        (fn [{:keys [element]}]
          (when (and (vector? element)
                     (identical? (first element) @#'ins/inspector*))
            (let [[_ context] element]
              (when (predicate? (:value context))
                context)))))))

(comment
  (require '[examples.data :refer [data]])

  (let [[a b] (find-location (atom nil) data any?)]
    (contains-context? a b))

  (take 10 (find-location (atom nil) data keyword?))

  #_(fn [{:keys [context context-set] :as vdom}]
      (for [{:keys [element] :as child-vdom} (:vdom-children vdom)
            :let [context (merge context context-set)]]
        (binding [lazy/*default-take* 10]
          (-> child-vdom
              (react/render* state context element)
              (assoc :context context)))))

  comment)

;; A user should be able to specify a find function.
;; - It should be able to receive all values in their data tree an indicate if it's interesting
;; - when a value is "found", a user should be able to jump directly to that value in it's context
;; - A user should be able to jump from found value to the next found value, or back to the previous found value
;; - filter is a crutch because find has been so challenging

;; How can you find a value in its rendered context?
;; Option 1. Render in the background a hidden react tree to materialize (context / location)
;; - should the find function be able to specify if children values are of interesting? (tree-seq style?)
;; - layout is expensive, but hidden rendering requires no layout
;; - how can we ensure that state doesn't cross between hidden and viewed components?  
;;   - never use globals, always use context to share state
;; - find can be on input data or rendered view?
;; - can we use portal.runtime.react for this? YES!

;; Start with rpc or ssr?

;; now that I have find figured out and working well, the next two pieces are around UX
;; How does the UI react when finding things?
;; How do a user specify what to find, and how is it different from filtering?
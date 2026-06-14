(ns portal.ui.find
  (:require [portal.runtime.react :as react]
            [portal.ui.inspector :as ins]
            [portal.ui.lazy :as lazy]
            [portal.ui.select :as select]
            [portal.ui.state :as state]
            [portal.ui.theme :as theme]))

(defn- inspector [state context value]
  [theme/with-theme
   :portal.colors/nord
   [theme/with-theme+
    {:max-depth 100
     ::lazy/default-take 1000
     ::ins/default-expand true}
    [state/with-state
     (atom @state)
     [select/with-index (-> context meta ::select/position)
      [ins/with-parent context
       [ins/with-context context
        [ins/inspector* context value]]]]]]])

(defn find-location [state context]
  (let [{:keys [value]} context]
    (->> [inspector state context value]
         (react/render {:state (atom {::react/lazy true})})
         (meta)
         (tree-seq :element :vdom-children)
         (keep
          (fn [{:keys [element]}]
            (when (and (vector? element)
                       (identical? (first element) @#'ins/inspector*))
              (second element)))))))

;; (defn- tag [hiccup]
;;   (when (vector? hiccup) (first hiccup)))

;; (declare find-location*)

;; (defn- find-element [out predicate? {:keys [element]}]
;;   ;; (tap> [::find-element vdom])
;;   (if-not (identical? (tag element) @#'ins/inspector*)
;;     out
;;     (let [[_ context] element]
;;       (cond-> out
;;         (predicate? (:value context))
;;         (conj context)))))

;; (defn- find-children [out predicate? {:keys [vdom-children]}]
;;   ;; (tap> [::find-children vdom-children])
;;   (reduce
;;    (fn [out vdom]
;;      (find-location* out predicate? vdom))
;;    out
;;    vdom-children))

;; (defn- find-location* [out predicate? vdom]
;;   (-> out
;;       (find-element predicate? vdom)
;;       (find-children predicate? vdom)))

;; (defn find-location-2 [state context predicate?]
;;   (let [{:keys [value]} context]
;;     (->> [inspector state context value]
;;          (react/render {:state (atom {::react/lazy true})})
;;          (meta)
;;          (find-location* [] predicate?))))

;; (comment
;;   (require '[examples.data :refer [data]])
;;   (count
;;    (find-location
;;     (atom {})
;;     {:value (repeat 25 data) :depth 0 :path [] :stable-path [] :alt-bg false}
;;     keyword?)) ;; 15.88s
;;   (count
;;    (find-location-2
;;     (atom {})
;;     {:value (repeat 25 data) :depth 0 :path [] :stable-path [] :alt-bg false}
;;     keyword?)) ;; 12.98s - takes ~80% of the time
;;   comment)

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
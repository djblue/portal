(ns ^:no-doc portal.ui.viewer.spec
  (:require [clojure.spec.alpha :as s]
            [portal.colors :as c]
            [portal.ui.icons :as icons]
            [portal.ui.inspector :as ins]
            [portal.ui.select :as select]
            [portal.ui.styled :as d]
            [portal.ui.theme :as theme]))

;;; :spec
(s/def ::path vector?)
(s/def ::pred any?)
(s/def ::val any?)
(s/def ::via vector?)
(s/def ::in vector?)

(s/def ::problem (s/keys :req-un [::path ::pred ::val ::via ::in]))

(s/def :clojure.spec.alpha/problems (s/coll-of ::problem))
(s/def :clojure.spec.alpha/spec any?)
(s/def :clojure.spec.alpha/value any?)

(s/def ::explain-data
  (s/keys :req [:clojure.spec.alpha/problems
                :clojure.spec.alpha/spec
                :clojure.spec.alpha/value]))
;;;

(defn- missing-key [{:keys [pred]}]
  (and (seq? pred)
       (seq? (nth pred 2))
       (= 'clojure.core/contains? (first (nth pred 2)))))

(defn- problem-dispatch [problem]
  (try
    (cond
      (missing-key problem) ::missing-key)
    (catch :default _)))

(defmulti inspect-problem #'problem-dispatch)

(defmethod inspect-problem ::missing-key [problem]
  (let [theme (theme/use-theme)
        k (nth (nth (:pred problem) 2) 2)]
    [ins/with-key
     :pred
     [d/div
      [d/span {:style {:color (::c/exception theme)}} "Missing key"]
      [ins/inspector k]]]))

(defmethod inspect-problem :default [{:keys [pred]}]
  (let [theme (theme/use-theme)]
    [ins/with-key
     :pred
     [theme/with-theme+
      {::c/symbol (::c/exception theme)}
      [ins/inspector pred]]]))

(defn with-keys [ks & children]
  (reduce
   (fn [out k]
     [ins/inc-depth [ins/with-key k out]])
   (into [:<>] children)
   (reverse ks)))

(defn inspect-spec [spec]
  (let [theme (theme/use-theme)]
    [d/div
     {:style
      {:background (ins/get-background)
       :border-radius (:border-radius theme)
       :border [1 :solid (::c/border theme)]}}
     [d/div
      {:style
       {:display :flex
        :justify-content :space-between
        :padding (:padding theme)
        :border-bottom [1 :solid (::c/border theme)]}}
      [icons/times-circle {:style {:color (::c/exception theme)}}]
      [d/div {:style {:color (::c/exception theme)}} "Spec Failure"]
      [d/div
       [select/with-position
        {:row -1 :column 0}
        [ins/with-key
         :clojure.spec.alpha/spec
         [ins/inspector (:clojure.spec.alpha/spec spec)]]]]]
     [d/div
      {:style {:display :grid
               :grid-template-columns "min-content min-content min-content auto"}}
      [d/div
       {:style
        {:padding (:padding theme)
         :border-right [1 :solid (::c/border theme)]}}
       [ins/inspector :spec]]
      [d/div
       {:style
        {:padding (:padding theme)
         :border-right [1 :solid (::c/border theme)]}}
       [ins/inspector :pred]]
      [d/div
       {:style
        {:padding (:padding theme)
         :border-right [1 :solid (::c/border theme)]}}
       [ins/inspector :val]]
      (map-indexed
       (fn [idx problem]
         ^{:key idx}
         [:<>
          [d/div
           {:style
            {:grid-column "1"
             :padding (:padding theme)
             :border-right [1 :solid (::c/border theme)]
             :border-top [1 :solid (::c/border theme)]}}
           [select/with-position
            {:row idx :column 0}
            [ins/with-key
             :clojure.spec.alpha/problems
             [ins/with-key idx
              [ins/with-key
               :via
               [ins/with-key
                (dec (count (:via problem)))
                [ins/inspector (last (:via problem))]]]]]]]
          [d/div
           {:style
            {:grid-column "2"
             :padding (:padding theme)
             :border-right [1 :solid (::c/border theme)]
             :border-top [1 :solid (::c/border theme)]}}
           [select/with-position
            {:row idx :column 2}
            [ins/with-key
             :clojure.spec.alpha/problems
             [ins/with-key idx
              [inspect-problem problem]]]]]
          [select/with-position
           {:row idx :column 3}
           [d/div
            {:style
             {:grid-column "3"
              :padding (:padding theme)
              :border-right [1 :solid (::c/border theme)]
              :border-top [1 :solid (::c/border theme)]}}
            [with-keys
             (:in problem)
             [ins/inspector (:val problem)]]]]])
       (:clojure.spec.alpha/problems spec))
      [d/div
       {:style {:grid-column "4"
                :grid-row (str "1 / span " (inc (count (:clojure.spec.alpha/problems spec))))
                :display :flex
                :justify-content :center
                :background (ins/get-background2)}}
       [ins/toggle-bg
        [select/with-position
         {:row 0 :column 4}
         [d/div
          {:style {:padding (* 2 (:padding theme))}}
          [ins/inspector
           {:portal.viewer/inspector {:expanded 100}}
           (:clojure.spec.alpha/value spec)]]]]]]]))

(defn- can-view? [x]
  (s/valid? ::explain-data x))

(def viewer
  {:predicate can-view?
   :component #'inspect-spec
   :name :portal.viewer/spec
   :doc "A viewer for data produced via clojure.spec.alpha/explain-data"})
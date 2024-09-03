(ns ^:no-doc portal.ui.viewer.test-report
  (:require [clojure.spec.alpha :as s]
            [portal.colors :as c]
            [portal.ui.filter :as-alias f]
            [portal.ui.icons :as icons]
            [portal.ui.inspector :as ins]
            [portal.ui.select :as select]
            [portal.ui.styled :as d]
            [portal.ui.theme :as theme]
            [portal.ui.viewer.source-location :as src]))

;;; :spec
(s/def :test-run/type #{:end-run-tests})

(s/def ::test-run
  (s/keys :req-un [:test-run/type]))

(s/def :test-ns/type #{:begin-test-ns :end-test-ns})

(s/def ::test-ns
  (s/keys :req-un [:test-ns/type ::ns]))

(s/def :test-var/type #{:begin-test-var :end-test-var})

(s/def ::test-var
  (s/keys :req-un [:test-var/type ::var]))

(s/def :assert/type #{:pass :fail :error})

(s/def ::assert
  (s/keys :req-un [:assert/type]
          :opt-un [::file ::line ::actual ::expected ::message]))

(s/def :summary/type #{:summary})

(s/def ::pass  number?)
(s/def ::fail  number?)
(s/def ::error number?)
(s/def ::test  number?)

(s/def ::summary
  (s/keys :req-un [:summary/type ::pass ::fail ::error ::test]))

(s/def ::output
  (s/or :run     ::test-run
        :ns      ::test-ns
        :var     ::test-var
        :assert  ::assert
        :summary ::summary))

(s/def ::report
  (s/coll-of ::output :min-count 1))
;;;

(defn- label [{:keys [message expected var ns type] :as value}]
  [d/div
   {:style {:flex "1"}}
   [ins/with-collection
    value
    [select/with-position
     {:row -1 :column 0}
     (cond
       (coll? message)
       [ins/with-key
        :message
        [ins/inspector
         (cond-> message
           (coll? message)
           (with-meta
             {:portal.viewer/default
              :portal.viewer/pr-str}))]]

       message
       [ins/with-key :message [ins/inspector message]]

       expected
       [ins/with-key
        :expected
        [ins/inspector
         (cond-> expected
           (coll? expected)
           (with-meta
             {:portal.viewer/default
              :portal.viewer/pr-str}))]]

       var
       [ins/with-key :var [ins/inspector var]]

       ns
       [ins/with-default-viewer
        :portal.viewer/pr-str
        [ins/with-key :ns [ins/inspector ns]]]

       type
       [ins/with-default-viewer
        :portal.viewer/pr-str
        [ins/with-key :type [ins/inspector type]]])]]])

(defn- inspect-assertion [value]
  (let [theme      (theme/use-theme)
        options    (ins/use-options)
        expanded?  (:expanded? options)
        background (ins/get-background)
        color      (case (:type value)
                     :pass (::c/diff-add theme)
                     (:error :fail) (::c/diff-remove theme)
                     (::c/border theme))]
    [d/div
     [d/div
      {:style
       {:display        :flex
        :flex-direction :row
        :align-items    :stretch
        :background     background}}
      [d/div
       {:style
        {:padding    (* 0.75 (:padding theme))
         :background color

         :border-top    [1 :solid color]
         :border-bottom [1 :solid color]
         :border-right  [1 :solid color]

         :border-top-left-radius    (:border-radius theme)
         :border-bottom-left-radius (when-not expanded? (:border-radius theme))}}
       [(case (:type value)
          :pass           icons/check-circle
          :fail           icons/times-circle
          :error          icons/times-circle
          :begin-test-ns  icons/play-circle
          :end-test-ns    icons/stop-circle
          :begin-test-var icons/play-circle
          :end-test-var   icons/stop-circle
          icons/info-circle)
        {:style {:color background}}]]
      [d/div
       {:style
        {:flex          "1"
         :display       :flex
         :gap           (:padding theme)
         :align-items   :center
         :padding       (* 0.5 (:padding theme))
         :border-top    [1 :solid (::c/border theme)]
         :border-bottom (when-not expanded? [1 :solid (::c/border theme)])
         :border-right  [1 :solid (::c/border theme)]

         :border-top-right-radius    (:border-radius theme)
         :border-bottom-right-radius (when-not expanded? (:border-radius theme))}}
       [ins/toggle-expand {:padding-left (:padding theme)}]
       [label value]
       (when-let [location (src/->source-location value)]
         [d/div
          [select/with-position
           {:row -1 :column 1}
           [ins/with-key :loc [ins/inspector location]]]])]]

     (when (:expanded? options)
       [ins/inspect-map-k-v (dissoc value :type :message :file :line :ns :var)])]))

(def ^:private begin? #{:begin-test-ns :begin-test-var})

(defn- summary [rows]
  (let [counts (frequencies (map :type rows))]
    (assoc counts :type (if (or (:error counts) (:fail counts))
                          :fail
                          :pass))))

(def ^:private empty-vec ^{:portal.viewer/default :portal.viewer/inspector} [])

(defn- end-test-var [{:keys [test-ns test-var] :as state}]
  (cond-> state
    test-var
    (-> (dissoc :test-var :asserts)
        (update :vars
                (fnil conj empty-vec)
                (merge
                 (summary (:asserts state))
                 {:ns      test-ns
                  :var     test-var
                  :asserts (:asserts state 0)})))))

(defn- end-test-ns [{:keys [test-ns vars] :as state}]
  (-> state
      (dissoc :test-ns :vars)
      (update :results
              (fnil conj empty-vec)
              (merge
               (summary vars)
               {:ns   test-ns
                :vars vars}))))

(defn- get-results [value]
  (let [include? (when value (into #{} value))]
    (:results
     (reduce
      (fn [{:keys [test-ns test-var] :as state} row]
        (case (:type row)
          :summary        state
          :begin-test-ns  (assoc state :test-ns (:ns row))
          :end-test-ns    (-> state end-test-var end-test-ns)
          :begin-test-var (assoc state :test-var (:var row))
          :end-test-var   (end-test-var state)
          (if (and value (not (include? row)))
            state
            (update
             state
             :asserts
             (fnil conj empty-vec)
             (cond-> row
               test-ns  (assoc :ns test-ns)
               test-var (assoc :var test-var))))))
      {:results empty-vec}
      value))))

(defn- group-assertions [value]
  (let [results (get-results value)]
    (if (= 1 (count results))
      (first results)
      (merge
       (summary results)
       {:message :test-report
        :results results}))))

(defn- inspect-test-report [value]
  [inspect-assertion (group-assertions value)])

(defn get-component [value]
  (cond
    (and (s/valid? ::report value)
         (begin? (:type (first value))))
    inspect-test-report

    (and (s/valid? ::output value)
         (not= (:type value) :summary))
    inspect-assertion))

(defn inspect-test [value]
  (let [component (get-component value)]
    [component value]))

(def viewer
  {:predicate get-component
   :component #'inspect-test
   :name      :portal.viewer/test-report
   :doc       "View clojure.test report output."})

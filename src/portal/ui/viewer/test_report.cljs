(ns portal.ui.viewer.test-report
  (:require [clojure.spec.alpha :as sp]
            [portal.colors :as c]
            [portal.ui.icons :as icons]
            [portal.ui.inspector :as ins]
            [portal.ui.select :as select]
            [portal.ui.styled :as s]
            [portal.ui.theme :as theme]))

(sp/def :test-ns/type #{:begin-test-ns :end-test-ns})

(sp/def ::test-ns
  (sp/keys :req-un [:test-ns/type ::ns]))

(sp/def :test-var/type #{:begin-test-var :end-test-var})

(sp/def ::test-var
  (sp/keys :req-un [:test-var/type ::var]))

(sp/def :assert/type #{:pass :fail :error})

(sp/def ::assert
  (sp/keys :req-un [:assert/type]
           :opt-un [::file ::line ::actual ::expected ::message]))

(sp/def :summary/type #{:summary})

(sp/def ::pass  number?)
(sp/def ::fail  number?)
(sp/def ::error number?)
(sp/def ::test  number?)

(sp/def ::summary
  (sp/keys :req-un [:summary/type ::pass ::fail ::error ::test]))

(sp/def :other/type keyword?)

(sp/def ::other
  (sp/keys :req-un [:other/type]))

(sp/def ::type
  (sp/or :ns      :test-ns/type
         :var     :test-var/type
         :assert  :assert/type
         :summary :summary/type))

(sp/def ::output
  (sp/or :ns      ::test-ns
         :var     ::test-var
         :assert  ::assert
         :summary ::summary
         :other   ::other))

(sp/def ::report
  (sp/coll-of ::output :min-count 1))

(defn- label [{:keys [type message] :as value}]
  [s/div
   {:style {:flex "1"}}
   [ins/with-key
    :label
    [select/with-position
     {:row -1 :column 0}
     [ins/inspector
      (or (when-not (sp/valid? ::type type)
            type)
          (if-not (coll? message)
            message
            (with-meta
              message
              {:portal.viewer/default
               :portal.viewer/pr-str}))
          (when-let [expected (:expected value)]
            (with-meta
              expected
              {:portal.viewer/default
               :portal.viewer/pr-str}))
          (:var value)
          (:ns value))]]]])

(defn- inspect-assertion [value]
  (let [theme      (theme/use-theme)
        options    (ins/use-options)
        expanded?  (:expanded? options)
        background (ins/get-background)
        color      (case (:type value)
                     :pass (::c/diff-add theme)
                     (:error :fail) (::c/diff-remove theme)
                     (::c/border theme))]
    [s/div
     [s/div
      {:style
       {:display        :flex
        :flex-direction :row
        :align-items    :stretch
        :background     background}}
      [s/div
       {:style
        {:padding    (:padding theme)
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
          :summary        icons/info-circle
          icons/info-circle)
        {:style {:color background}}]]
      [s/div
       {:style
        {:flex          "1"
         :display       :flex
         :gap           (:padding theme)
         :align-items   :center
         :padding       (:padding theme)
         :border-top    [1 :solid (::c/border theme)]
         :border-bottom (when-not expanded? [1 :solid (::c/border theme)])
         :border-right  [1 :solid (::c/border theme)]

         :border-top-right-radius    (:border-radius theme)
         :border-bottom-right-radius (when-not expanded? (:border-radius theme))}}
       [label value]
       (when-let [file (:file value)]
         [s/div
          {:style {:color (::c/uri theme)}}
          file
          (when-let [line (:line value)]
            [:<> ":" line])])]]

     (when (:expanded? options)
       [ins/inspect-map-k-v (dissoc value :type :message :file :line :ns :var)])]))

(def ^:private begin? #{:begin-test-ns :begin-test-var})

(defn- summary [rows]
  (let [counts (frequencies (map :type rows))]
    (assoc counts :type (if (or (:error counts) (:fail counts))
                          :fail
                          :pass))))

(def ^:private empty-vec ^{:portal.viewer/default :portal.viewer/inspector} [])

(defn- get-results [value]
  (:results
   (reduce
    (fn [{:keys [test-ns test-var] :as state} row]
      (case (:type row)
        :summary        state
        :begin-test-ns  (assoc  state :test-ns (:ns row))
        :end-test-ns    (-> state
                            (dissoc :test-ns :vars)
                            (update :results
                                    (fnil conj empty-vec)
                                    (merge
                                     (summary (:vars state))
                                     {:ns   test-ns
                                      :vars (:vars state)})))
        :begin-test-var (assoc  state :test-var (:var row))
        :end-test-var   (-> state
                            (dissoc :test-var :asserts)
                            (update :vars
                                    (fnil conj empty-vec)
                                    (merge
                                     (summary (:asserts state))
                                     {:ns      test-ns
                                      :var     test-var
                                      :asserts (:asserts state)})))
        (:pass :fail :error)
        (update
         state
         :asserts
         (fnil conj empty-vec)
         (cond-> row
           test-ns  (assoc :ns test-ns)
           test-var (assoc :var test-var)))

        (update-in
         state
         [:asserts (dec (count (:asserts state)))]
         merge
         (dissoc row :type))))
    {:results empty-vec}
    value)))

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
    (and (sp/valid? ::report value)
         (begin? (:type (first value))))
    inspect-test-report

    (and (sp/valid? ::output value)
         (not= (:type value) :summary))
    inspect-assertion))

(defn inspect-test [value]
  (let [component (get-component value)]
    [component value]))

(def viewer
  {:predicate get-component
   :component inspect-test
   :name      :portal.viewer/test-report})

(ns portal.ui.viewer.exception
  (:require [clojure.spec.alpha :as spec]
            [clojure.string :as str]
            [portal.colors :as c]
            [portal.ui.styled :as s]))

(spec/def ::cause string?)

(spec/def ::trace-line
  (spec/cat :class symbol?
            :method symbol?
            :file (spec/or :str string? :nil nil?)
            :line number?))

(spec/def ::trace (spec/coll-of ::trace-line))

(spec/def ::type symbol?)
(spec/def ::message string?)
(spec/def ::at ::trace-line)

(spec/def ::via
  (spec/coll-of
   (spec/keys :req-un [::type ::message ::at])))

(spec/def ::exception
  (spec/keys :req-un [::cause ::trace ::via]))

(defn exception? [value]
  (spec/valid? ::exception value))

(defn get-background [settings]
  (if (even? (:depth settings))
    (::c/background settings)
    (::c/background2 settings)))

(defn- format-trace-line [class method]
  (let [split (str/split class #"\$")]
    (if (and ('#{invokeStatic invoke doInvoke} method)
             (< 1 (count split)))
      [(str/join "/" (drop-last split)) (last split)]
      [class method])))

(defn- inspect-trace-line [settings trace-line]
  (let [[class method file line] trace-line]
    [:<>
     (when file
       [s/div {:style
               {:grid-column "1"
                :text-align :right
                :color (::c/string settings)}}
        (pr-str file)])
     [s/div {:style
             {:grid-column "2"
              :text-align :right
              :color (::c/number settings)}}
      line]
     (let [[class method] (format-trace-line class method)]
       [s/div {:style
               {:grid-column "3"}}
        [s/span {:style {:color (::c/namespace settings)}} class "/"]
        [s/span {:style {:color (::c/symbol settings)}} method]])]))

(defn inspect-exception [settings value]
  [s/div
   {:style
    {:background (get-background settings)
     :margin "0 auto"
     :padding (:spacing/padding settings)
     :box-sizing :border-box
     :color (::c/text settings)
     :font-size  (:font-size settings)
     :border-radius (:border-radius settings)
     :border (str "1px solid " (::c/border settings))}}
   (let [{:keys [via trace]} value]
     [s/div
      {:style
       {:display :flex
        :justify-content :center}}
      [s/div
       {:style {:display    :grid
                :grid-gap   (:spacing/padding settings)
                :grid-template-columns "auto auto auto"}}
       (map-indexed
        (fn [idx {:keys [type message at]}]
          ^{:key idx}
          [:<>
           [s/div
            {:style
             {:text-align :right
              :grid-column "1"
              :font-weight :bold
              :color (::c/exception settings)}}
            type]
           [s/div
            {:style {:grid-column "3"}}
            message]
           [inspect-trace-line settings at]])
        via)
       (map-indexed
        (fn [idx trace-line]
          ^{:key idx} [inspect-trace-line settings trace-line])
        (rest trace))]])])


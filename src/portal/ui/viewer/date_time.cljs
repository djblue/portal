(ns portal.ui.viewer.date-time
  (:require [clojure.spec.alpha :as s]
            [portal.colors :as c]
            [portal.ui.styled :as d]
            [portal.ui.theme :as theme]))

;;; :spec
(s/def ::date-time
  (s/or :inst     inst?
        :unix     number?
        :iso-8601 string?))
;;;

(defn parse [date]
  (cond
    (inst? date) date

    ;; unix timestamps
    (number? date)
    (js/Date. (* date 1000))

    (string? date)
    (let [date (.parse js/Date date)]
      (when-not (js/isNaN date) (js/Date. date)))))

(def ^:private days
  ["Sunday"
   "Monday"
   "Tuesday"
   "Wednesday"
   "Thursday"
   "Friday"
   "Saturday"])

(def ^:private months
  ["January"
   "February"
   "March"
   "April"
   "May"
   "June"
   "July"
   "August"
   "September"
   "October"
   "November"
   "December"])

(defn inspect-time [value]
  (when-let [value (parse value)]
    (let [hour    (.getHours value)
          minute  (.getMinutes value)
          second  (.getSeconds value)
          theme   (theme/use-theme)
          style   {:color (::c/number theme)}
          border  {:color (::c/border theme)}
          keyword {:color (::c/keyword theme)}]
      [d/div
       {:title "Time"
        :style
        {:display :flex
         :align-items :center}}
       [d/span
        {:title "Hour" :style style}
        (let [hour (mod hour 12)]
          (if (= hour 0) 12 hour))]
       [d/span {:style border} ":"]
       [d/span
        {:title "Minute" :style style}
        (when (< minute 10) "0") minute]
       [d/span {:style border} ":"]
       [d/span
        {:title "Second" :style style}
        (when (< second 10) "0")
        second]
       [d/div {:style {:width "0.25em"}}]
       [d/span
        {:style keyword}
        (if (> hour 11) "PM" "AM")]])))

(defn inspect-date [value]
  (let [value   (parse value)
        day     (.getDay value)
        date    (.getDate value)
        month   (.getMonth value)
        year    (.getFullYear value)
        theme   (theme/use-theme)
        style   {:color (::c/number theme)}
        border  {:color (::c/border theme)}]
    [d/div
     {:title "Date"
      :style
      {:display :flex
       :align-items :center}}
     [d/span
      {:title (nth months month) :style style}
      (inc month)]
     [d/span {:style border} "/"]
     [d/span
      {:title (nth days day) :style style}
      (when (< date 10) "0") date]
     [d/span {:style border} "/"]
     [d/span
      {:title "Year" :style style}
      year]]))

(defn inspect-date-time [value]
  (let [value (parse value)]
    [d/div
     {:style
      {:display :flex
       :align-items :center}}
     [inspect-date value]
     [d/div {:style {:width "0.75em"}}]
     [inspect-time value]]))

(def viewer
  {:predicate parse
   :component inspect-date-time
   :name :portal.viewer/date-time})

(ns portal.ui.viewer.date-time
  (:require [portal.colors :as c]
            [portal.ui.styled :as s]
            [portal.ui.theme :as theme]))

(defn date? [value] (instance? js/Date value))

(defn parse [date]
  (if (date? date)
    date
    (let [date (.parse js/Date date)]
      (when-not (js/isNaN date) (js/Date. date)))))

(def ^:private days
  ["Monday"
   "Tuesday"
   "Wednesday"
   "Thursday"
   "Friday"
   "Saturday"
   "Sunday"])

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

(defn inspect-date-time [value]
  (let [value   (parse value)
        day     (.getDay value)
        date    (.getDate value)
        month   (.getMonth value)
        year    (.getFullYear value)
        hour    (.getHours value)
        minute  (.getMinutes value)
        second  (.getSeconds value)
        theme   (theme/use-theme)
        style   {:color (::c/number theme)}
        border  {:color (::c/border theme)}
        keyword {:color (::c/keyword theme)}]
    [s/div
     {:title "Date"
      :style
      {:display :flex
       :align-items :center}}
     [s/div
      {:style
       {:display :flex
        :align-items :center}}
      [s/span
       {:title (nth months month) :style style}
       (inc month)]
      [s/span {:style border} "/"]
      [s/span
       {:title (nth days (dec day)) :style style}
       (when (< date 10) "0") date]
      [s/span {:style border} "/"]
      [s/span
       {:title "Year" :style style}
       year]]

     [s/div {:style {:width "0.75em"}}]

     [s/div
      {:title "Time"
       :style
       {:display :flex
        :align-items :center}}
      [s/span
       {:title "Hour" :style style}
       (mod hour 12)]
      [s/span {:style border} ":"]
      [s/span
       {:title "Minute" :style style}
       (when (< minute 10) "0") minute]
      [s/span {:style border} ":"]
      [s/span
       {:title "Second" :style style}
       (when (< second 10) "0")
       second]
      [s/div {:style {:width "0.25em"}}]
      [s/span
       {:style keyword}
       (if (> hour 12) "PM" "AM")]]]))

(def viewer
  {:predicate parse
   :component inspect-date-time
   :name :portal.viewer/date-time})

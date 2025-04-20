(ns ^:no-doc portal.ui.viewer.relative-time
  (:refer-clojure :exclude [second])
  (:require [clojure.spec.alpha :as s]
            [portal.ui.react :as react]
            [portal.ui.styled :as d]
            [portal.ui.viewer.date-time :as date-time]))

;;; :spec
(s/def ::relative-time
  (s/or :inst     inst?
        :unix     number?
        :iso-8601 string?))
;;;

(def ^:private millisecond 1)
(def ^:private second     (* 1000 millisecond))
(def ^:private minute     (* 60 second))
(def ^:private hour       (* 60 minute))
(def ^:private day        (* 24 hour))
(def ^:private week       (* 7 day))
(def ^:private month      (* 30 day))
(def ^:private year       (* 365 day))

(def ^:private time-scales
  [:year        year
   :month       month
   :week        week
   :day         day
   :hour        hour
   :minute      minute
   :second      second
   :millisecond millisecond])

(defn- relative-time
  ([b]
   (relative-time (js/Date.) b))
  ([a b]
   (let [diff (- (.getTime b) (.getTime a))
         ms   (Math/abs diff)]
     (some
      (fn [[unit scale]]
        (when (> ms scale)
          {:scale (Math/floor (/ ms scale))
           :unit  unit
           :direction
           (cond
             (neg? diff) :past
             (pos? diff) :future
             :else       :now)}))
      (partition 2 time-scales)))))

(defn- format-relative-time [{:keys [scale unit direction]}]
  (if (= direction :now)
    "now"
    (str scale
         " "
         (name unit)
         (when (> scale 1) "s")
         " "
         (case direction
           :past "ago"
           :future "in the future"))))

(defn inspect-relative [value]
  (let [value          (date-time/parse value)
        [now set-now!] (react/use-state (js/Date.))]
    (react/use-effect
     :once
     (let [i (js/setInterval
              (fn []
                (set-now! (js/Date.)))
              1000)]
       (fn []
         (js/clearInterval i))))
    [d/div (format-relative-time (relative-time now value))]))

(def viewer
  {:predicate date-time/parse
   :component #'inspect-relative
   :name :portal.viewer/relative-time
   :doc "View value relative to the current time."})

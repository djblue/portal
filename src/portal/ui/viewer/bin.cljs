(ns ^:no-doc portal.ui.viewer.bin
  (:require [portal.colors :as c]
            [portal.ui.inspector :as ins]
            [portal.ui.lazy :as l]
            [portal.ui.styled :as s]
            [portal.ui.theme :as theme]))

(def ^:private indexed (partial map-indexed vector))

(defn- hex-value [[a b]]
  (let [theme (theme/use-theme)]
    [s/div
     {:style {:color (::c/number theme)}}
     (when a (.padStart (.toString a 16) 2 "0"))
     (when b (.padStart (.toString b 16) 2 "0"))]))

(defn- pad-8 [row]
  (take 8 (concat row (repeat nil))))

(defn- inspect-hex [value]
  (for [[idx row] (indexed (partition-all 16 value))]
    [:<>
     {:key idx}
     (for [[idx hex] (indexed (pad-8 (partition-all 2 row)))]
       ^{:key idx} [hex-value hex])]))

(defn- ascii-value [c]
  (let [theme (theme/use-theme)]
    (if-not (<= 32 c 127)
      [s/div {:style {:color (::c/border theme)}} "."]
      [s/div {:style {:color (::c/string theme)}}
       (String/fromCharCode c)])))

(defn- inspect-ascii [value]
  (for [[idx row] (indexed (partition-all 16 value))]
    [:<>
     {:key idx}
     (for [[idx c] (indexed row)]
       ^{:key idx} [ascii-value c])]))

(defn inspect-bin [value]
  (let [theme (theme/use-theme)
        hex   (inspect-hex value)
        ascii (inspect-ascii value)
        opts  (ins/use-options)]
    [s/div
     {:style {:display :flex
              :overflow :auto
              :max-height (when-not (:expanded? opts) "24rem")}}
     [s/div
      {:style {:display :grid
               :grid-gap (:padding theme)
               :grid-template-columns
               (str "repeat(" (+ 1 1 8 1 16) ", auto)")}}
      (l/use-lazy
       value
       (for [[idx [hex ascii]] (indexed (map vector hex ascii))]
         [:<> {:key idx}
          [s/div
           {:style {:color (::c/border theme)}}
           (.padStart (.toString idx 16) 8 "0") ":"]
          [s/div]
          [:<> hex]
          [s/div]
          [:<> ascii]]))]]))

(def viewer
  {:predicate ins/bin?
   :component #'inspect-bin
   :name      :portal.viewer/bin
   :doc       "View binary data as a hexdump."})

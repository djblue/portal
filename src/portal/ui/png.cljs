(ns portal.ui.png
  (:require ["png-chunk-text" :as text]
            ["png-chunks-encode" :as png-encode]
            ["png-chunks-extract" :as png-extract]))

(defn encode [png data]
  (let [chunks (png-extract png)]
    (doseq [[keyword content] data]
      (.splice chunks -1 0 (text/encode keyword content)))
    (png-encode chunks)))

(defn decode [png]
  (let [chunks (png-extract png)]
    (persistent!
     (reduce
      (fn [m ^js chunk]
        (if-not (= "tEXt" (.-name chunk))
          m
          (let [data (text/decode chunk)]
            (assoc! m (.-keyword data) (.-text data)))))
      (transient {})
      chunks))))
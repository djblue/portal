(ns portal.ui.parsers
  {:no-doc true})

(defmulti parse-string (fn [format _] format))

(defmethod parse-string :format/text [_ s] s)

(defn formats [] (keys (methods parse-string)))

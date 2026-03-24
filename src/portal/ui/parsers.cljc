(ns ^:no-doc portal.ui.parsers)

(defmulti parse-string (fn [format _] format))

(defmethod parse-string :format/text [_ s] s)

(defn formats [] (keys (methods parse-string)))
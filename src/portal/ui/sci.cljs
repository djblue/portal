(ns ^:no-doc portal.ui.sci
  (:require [sci.core :as sci]))

(def ctx (atom nil))

(defn eval-string [s]
  (try (sci/eval-string* @ctx s)
       (catch :default e
         (let [sci-error? (isa? (:type (ex-data e)) :sci/error)]
           (throw (if sci-error?
                    (or (ex-cause e) e)
                    e))))))

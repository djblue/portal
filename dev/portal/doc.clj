(ns portal.doc
  (:require [clojure.edn :as edn]
            [clojure.walk :as walk]
            [portal.api :as p]))

(defn gen-docs []
  (walk/postwalk
   (fn [v]
     (if-let [file (:file v)]
       (with-meta
         (assoc v :markdown (slurp file))
         {:portal.viewer/for
          {:markdown :portal.viewer/markdown}})
       v))
   (edn/read-string (slurp "doc/cljdoc.edn"))))

(comment
  (def docs (atom nil))
  (reset! docs (gen-docs))
  (reset! docs nil)
  (p/open {:mode :dev :window-title "portal-docs" :value docs}))

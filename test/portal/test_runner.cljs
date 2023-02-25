(ns portal.test-runner
  (:require [cljs.test :as t]
            [clojure.pprint :as pp]
            [portal.client.node :as p]
            [portal.runtime.bench-cson :as bench]
            [portal.runtime.cson-test]
            [portal.runtime.fs :as fs]
            [portal.runtime.fs-test]
            [portal.runtime.json :as json]
            [portal.runtime.json-buffer-test]
            [portal.runtime.npm-test]))

(defn- error->data [ex]
  (with-meta
    (merge
     (when-let [data (.-data ex)]
       {:data data})
     {:runtime :cljs
      :cause   (.-message ex)
      :via     [{:type    (symbol (.-name (type ex)))
                 :message (.-message ex)}]
      :stack   (.-stack ex)})
    {:portal.viewer/for
     {:stack :portal.viewer/text}}))

(extend-type js/Error
  IPrintWithWriter
  (-pr-writer [this writer _opts]
    (-write writer "#error ")
    (binding [*print-meta* true]
      (-write writer (error->data this)))))

(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
  (when-not (cljs.test/successful? m)
    (.exit js/process 1)))

(def port (.. js/process -env -PORTAL_PORT))

(defn submit [value] (p/submit {:port port} value))

(defn table [value]
  (if port
    (submit value)
    (pp/print-table
     (get-in (meta value) [:portal.viewer/table :columns])
     value)))

(defn run-tests [f]
  (if-not port
    (f)
    (let [report (atom [])
          counts
          (with-redefs [t/report #(swap! report conj %)]
            (f))]
      (submit @report)
      counts)))

(defn -main []
  (run-tests
   #(t/run-tests 'portal.runtime.cson-test
                 'portal.runtime.fs-test
                 'portal.runtime.json-buffer-test
                 'portal.runtime.npm-test))
  (table (bench/run (json/read (fs/slurp "package-lock.json")) 100)))

(-main)

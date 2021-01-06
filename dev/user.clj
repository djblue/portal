(ns user
  (:require [cheshire.core :as json]
            [clojure.core.protocols :refer [Datafiable]]
            [clojure.datafy :refer [datafy]]
            [clojure.java.io :as io]
            [examples.data :refer [data]]
            [portal.api :as p]
            [portal.runtime.jvm.launcher :as l]
            [portal.runtime.jvm.server :as s]
            [pwa]))

(defn cljs
  ([] (cljs :client))
  ([build-id]
   ((requiring-resolve 'shadow.cljs.devtools.server/start!))
   ((requiring-resolve 'shadow.cljs.devtools.api/watch)        build-id)
   ((requiring-resolve 'shadow.cljs.devtools.api/nrepl-select) build-id)))

(defn node [] (cljs :node))

(p/tap)

(extend-protocol Datafiable
  java.io.File
  (datafy [^java.io.File this]
    {:name          (.getName this)
     :absolute-path (.getAbsolutePath this)
     :flags         (->> [(when (.canRead this)     :read)
                          (when (.canExecute this)  :execute)
                          (when (.canWrite this)    :write)
                          (when (.exists this)      :exists)
                          (when (.isAbsolute this)  :absolute)
                          (when (.isFile this)      :file)
                          (when (.isDirectory this) :directory)
                          (when (.isHidden this)    :hidden)]
                         (remove nil?)
                         (into #{}))
     :size          (.length this)
     :last-modified (.lastModified this)
     :uri           (.toURI this)
     :files         (seq (.listFiles this))
     :parent        (.getParentFile this)}))

(defn swap-dev []
  (alter-var-root
   #'s/resource
   assoc "main.js" (io/file "target/resources/portal/main.js")))

(swap-dev)

(comment
  (with-redefs [l/pwa (:dev pwa/envs)]
    (def portal (p/open)))
  (p/tap)
  (tap> [{:hello :world :old-key 123} {:hello :youtube :new-key 123}])
  (doseq [i (range 100)] (tap> [::index i]))
  (p/clear)
  (p/close)

  (-> @portal)
  (tap> portal)
  (swap! portal * 1000)
  (reset! portal 1)

  (tap> (datafy java.io.File))
  ;; should cause an error in portal because of transit
  (tap> {(with-meta 'k {:a :b}) 'v})

  (tap> (with-meta (range) {:hello :world}))
  (tap> (json/parse-stream (io/reader "package-lock.json")))
  (tap> (io/file "deps.edn"))
  (tap> data))

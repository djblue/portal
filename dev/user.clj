(ns user
  (:require [examples.data :refer [data]]
            [portal.api :as p]
            [portal.runtime.server.jvm :as s]
            [cheshire.core :as json]
            [shadow.cljs.devtools.api :as shadow]
            [clojure.java.io :as io]
            [clojure.datafy :refer [datafy]]
            [clojure.core.protocols :refer [Datafiable]]))

(defn cljs [] (shadow/repl :client))

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

(comment
  (alter-var-root #'s/resource assoc "main.js" (io/file "target/resources/main.js"))

  (def portal (p/open))
  (p/tap)
  (tap> [{:hello :world :old-key 123} {:hello :youtube :new-key 123}])
  (p/clear)
  (p/close)

  (-> @portal)
  (tap> portal)
  (swap! portal * 1000)
  (reset! portal 1)

  (tap> (datafy java.io.File))
  ;; should cause an error in portal because of transit
  (tap> {(with-meta 'k {:a :b}) 'v})

  (tap> (json/parse-stream (io/reader "package-lock.json")))
  (tap> (io/file "deps.edn"))
  (tap> data))

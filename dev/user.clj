(ns user
  (:require [examples.data :refer [data]]
            [portal.api :as p]
            [clojure.java.io :as io]
            [clojure.datafy :refer [datafy]]
            [clojure.core.protocols :refer [Datafiable]]))

(comment
  (p/open)
  (p/tap)
  (tap> [{:hello :world :old-key 123} {:hello :youtube :new-key 123}])
  (p/clear)
  (p/close)

  (tap> (datafy java.io.File))

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

  (tap> (io/file "deps.edn"))
  (tap> data))

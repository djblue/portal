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

(defn lazy-fn [symbol]
  (fn [& args] (apply (requiring-resolve symbol) args)))

(def start!       (lazy-fn 'shadow.cljs.devtools.server/start!))
(def watch        (lazy-fn 'shadow.cljs.devtools.api/watch))
(def nrepl-select (lazy-fn 'shadow.cljs.devtools.api/nrepl-select))

(defn cljs
  ([] (cljs :client))
  ([build-id] (start!) (watch build-id) (nrepl-select build-id)))

(defn node [] (cljs :node))

(add-tap #'p/submit)

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
  (watch :pwa)

  (with-redefs [l/pwa (:dev pwa/envs)]
    (def portal (p/open)))
  (add-tap #'p/submit)
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

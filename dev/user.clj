(ns user
  (:require [clojure.core.protocols :refer [Datafiable]]
            [clojure.datafy :refer [datafy]]
            [clojure.java.io :as io]
            [examples.data :refer [data]]
            [portal.api :as p]
            [portal.runtime.json :as json]
            [tracker]))

(defn lazy-fn [symbol]
  (fn [& args] (apply (requiring-resolve symbol) args)))

(def start!       (lazy-fn 'shadow.cljs.devtools.server/start!))
(def watch        (lazy-fn 'shadow.cljs.devtools.api/watch))
(def repl         (lazy-fn 'shadow.cljs.devtools.api/repl))

(defn cljs
  ([] (cljs :client))
  ([build-id] (start!) (watch build-id) (repl build-id)))

(defn node [] (cljs :node))

(add-tap #'p/submit)

(extend-protocol Datafiable
  java.io.File
  (datafy [^java.io.File this]
    {:name          (.getName this)
     :absolute-path (.getAbsolutePath this)
     :flags         (cond-> #{}
                      (.canRead this)     (conj :read)
                      (.canExecute this)  (conj :execute)
                      (.canWrite this)    (conj :write)
                      (.exists this)      (conj :exists)
                      (.isAbsolute this)  (conj :absolute)
                      (.isFile this)      (conj :file)
                      (.isDirectory this) (conj :directory)
                      (.isHidden this)    (conj :hidden))
     :size          (.length this)
     :last-modified (.lastModified this)
     :uri           (.toURI this)
     :files         (seq (.listFiles this))
     :parent        (.getParentFile this)}))

(comment
  (watch :pwa)

  (tracker/start)
  (tracker/stop)

  (def value (atom data))
  (reset! value data)
  (reset! value {})
  (reset! value {:hello :world})
  (reset! value (map #(-> [::index %]) (range 100)))
  (def portal (p/open))
  (def dev    (p/open {:mode :dev}))
  (def dev    (p/open {:mode :dev :value value}))
  (def code   (p/open {:mode :dev :editor :vs-code}))
  (def idea   (p/open {:mode :dev :launcher :intellij}))
  (def work   (p/open {:mode :dev :main 'workspace}))
  (def remote (p/open {:runtime {:type :socket :port 5555}}))
  (def remote (p/open {:runtime {:type :socket :port 6666}}))

  (p/eval-str "(portal.ui.commands/select-parent portal.ui.state/state)")

  (type (p/eval-str "#js {}"))
  (type (p/eval-str "{}"))
  (type (p/eval-str "(+ 1 2 3)"))

  (add-tap #'p/submit)
  (remove-tap #'p/submit)
  (tap> [{:hello :world :old-key 123} {:hello :youtube :new-key 123}])
  (doseq [i (range 100)] (tap> [::index i]))
  (p/clear)
  (p/close)

  (-> @dev)
  (tap> portal)
  (swap! portal * 1000)
  (reset! portal 1)

  (tap> (datafy java.io.File))

  (require '[portal.console :as log])

  (do (log/trace ::trace)
      (log/debug ::debug)
      (log/info  ::info)
      (log/warn  ::warn)
      (log/error ::error))

  (tap> 4611681620380904123)
  (tap> (with-meta (range) {:hello :world}))
  (tap> (json/read (slurp "package-lock.json")))
  (tap> (io/file "deps.edn"))
  (dotimes [_i 25] (tap> data)))

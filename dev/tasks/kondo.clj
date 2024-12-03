(ns tasks.kondo
  (:require
   [clj-kondo.core :as core]
   [clj-kondo.main :as kondo]
   [clojure.string :as str]
   [portal.client.jvm :as p]))

(defn- file->ns [file]
  (some-> file
          (str/replace #"src/|test/|dev/|.clj.?" "")
          (str/escape {\/ "." \_ "-"})
          symbol))

(defn- print! [f {:keys [findings] :as data}]
  (if-let [port (System/getenv "PORTAL_PORT")]
    (let [submit (partial p/submit {:port port :encoding :cson})]
      (doseq [{:keys [col row filename level message type]} findings]
        (submit
          {:level  (get {:warning :warn :fail :fatal} level level)
           :type   type
           :column col
           :ns     (file->ns filename)
           :line   row
           :file   filename
           :result message
           :time   (java.util.Date.)})))
    (f data)))

(def ^:private config
  {:output
   {:pattern "::{{level}} file={{filename}},line={{row}},col={{col}}::clj-kondo: {{message}} {{type}}"}})

(defn -main [& args]
  (with-redefs [core/print! (partial print! core/print!)]
    (apply kondo/-main
           (concat args
                   (when (System/getenv "GITHUB_ACTIONS")
                     ["--config" (pr-str config)])))))

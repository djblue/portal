(ns portal.e2e)

(defn step* [code]
  (binding [*out* *err*]
    (println "\n==> Enter to execute:" code "\n"))
  (read-line)
  (prn code))

(defmacro step [code]
  `(step* '~code))

(defn -main [& args]
  (if (= (first args) "web")
    (step (require '[portal.web :as p]))
    (step (require '[portal.api :as p])))
  (step (do (p/tap) (p/open)))
  (step (tap> :hello-world))
  (step (p/clear))
  (step (require '[examples.hacker-news :as hn]))
  (step (tap> hn/stories))
  (step (p/close)))

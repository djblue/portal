(ns tasks.jar
  (:require [clojure.tools.build.api :as b]
            [tasks.info :refer [options]]))

(defn -main []
  (let [{:keys [class-dir jar-file]} options]
    (b/copy-dir {:src-dirs   ["src/portal"]
                 :target-dir (str class-dir "/portal")})
    (b/copy-dir {:src-dirs   ["resources/portal"]
                 :target-dir (str class-dir "/portal")})
    (b/delete {:path (str class-dir "/portal/extensions/")})
    (b/jar {:class-dir class-dir :jar-file  jar-file})
    (shutdown-agents)))

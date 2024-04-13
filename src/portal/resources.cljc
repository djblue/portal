(ns ^:no-doc portal.resources
  #?(:cljs (:require-macros portal.resources))
  #?(:clj (:require [clojure.java.io :as io]))
  #?(:org.babashka/nbb (:require ["fs" :as fs]
                                 ["path" :as path])
     :joyride (:require ["vscode" :as vscode]
                        ["ext://djblue.portal$resources" :as resources])))

#?(:org.babashka/nbb (def file *file*))

(defn resource [_resource-name]
  #?(:org.babashka/nbb (some
                        (fn [file]
                          (when (fs/existsSync file) file))
                        [(path/join file "../../" _resource-name)
                         (path/join file "../../../resources" _resource-name)])
     :joyride
     (let [vscode  (js/require "vscode")
           path    (js/require "path")
           ^js uri (-> vscode .-workspace .-workspaceFolders (aget 0) .-uri)
           fs-path (.-fsPath uri)]
       (.join path (if-not (undefined? fs-path) fs-path (.-path uri))
              "resources"
              _resource-name))))

(defonce ^:no-doc resources (atom {}))

#?(:clj
   (defmacro inline
     "For runtime resources, ensure the inline call happens at the namespace
     top-level to ensure resources are pushed into `resources for use as part of
     the inline fn."
     [resource-name]
     (try
       `(-> resources
            (swap! assoc ~resource-name ~(slurp (io/resource resource-name)))
            (get ~resource-name))
       (catch Exception e
         (println e))))
   :joyride (defn inline [resourece-name] (resources/inline resourece-name))
   :org.babashka/nbb (defn inline [resource-name]
                       (fs/readFileSync (resource resource-name) "utf8"))
   :cljs    (defn inline [resourece-name] (get @resources resourece-name)))
(ns portal.runtime.index)

(def testing? (atom false))

(defn html [& {:keys [name version code-url platform]
               :or {name     "portal"
                    version  "0.9.0"
                    code-url "main.js"
                    platform #?(:bb "bb" :clj "jvm" :cljs "node")}}]
  (str
   "<head>"
   "<title>" (str name " - " platform " - " version) "</title>"
   "<meta charset='UTF-8' />"
   "<meta name='viewport' content='width=device-width, initial-scale=1' />"
   "</head>"
   "<body style=\"margin: 0; overflow: hidden\">"
   "<div id=\"root\"></div>"
   "<script src=\"" code-url "\"></script>"
   ;; wait.js will ensure headless chrome doesn't exit early
   (when @testing? "<script src=\"wait.js\"></script>")
   "</body>"))

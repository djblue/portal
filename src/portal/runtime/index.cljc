(ns ^:no-doc portal.runtime.index)

(defn html [{:keys [name version host port session-id code-url platform mode]
             :or   {name       "portal"
                    version    "0.59.2"
                    code-url   "main.js"
                    platform   #?(:bb "bb" :clj "jvm" :cljs "node" :cljr "clr" :lpy "py")}}]
  (str
   "<!DOCTYPE html>"
   "<html lang=\"en\">"
   "<head>"
   "<title>" (str name " - " platform " - " version) "</title>"
   "<meta charset='UTF-8' />"
   "<meta name='viewport' content='width=device-width, initial-scale=1' />"
   "<meta name='theme-color' content='' />"
   "<link rel=\"icon\" href=\"/icon.svg\">"
   "</head>"
   "<body style=\"margin: 0; padding: 0; height: 100vh\">"
   "<div id=\"root\"></div>"
   "<script>"
   (when host
     (str "window.PORTAL_HOST    = " (pr-str (str host ":" port)) ";"))
   (when session-id
     (str "window.PORTAL_SESSION = " (pr-str session-id) ";"))
   "</script>"
   "<script src=\"" code-url "\"></script>"
   ;; wait.js will ensure headless chrome doesn't exit early
   (when (= mode :test) "<script src=\"wait.js\"></script>")
   "</body>"
   "</html>"))

(ns ^:no-doc portal.runtime.index)

(defn html [{:keys [name version host port session-id code-url platform mode]
             :or   {name       "portal"
                    version    "0.63.1"
                    platform   #?(:bb "bb" :clj "jvm" :cljs "node" :cljr "clr" :lpy "py")}}]
  (let [server-url (when (and host port) (str "http://" host ":" port))
        code-url   (or code-url (str server-url "/main.js?" session-id))]
    (str
     "<!DOCTYPE html>"
     "<html lang=\"en\">"
     "<head>"
     "<title>" name " - " platform " - " version "</title>"
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
       (str "window.PORTAL_SESSION = " (pr-str (str session-id)) ";"))
     "</script>"
     (if (= mode :ssr)
       (str "<script src=\"https://cdn.jsdelivr.net/npm/scittle@0.8.31/dist/scittle.js\"
                     type=\"application/javascript\"></script>"
            "<script src=\"https://unpkg.com/idiomorph@0.7.4\"></script>"
            "<script type=\"application/x-scittle\" src=\"" server-url "/main.cljs\"></script>")
       (str "<script src=\"" code-url "\"></script>"))
     ;; wait.js will ensure headless chrome doesn't exit early
     (when (= mode :test) "<script src=\"wait.js\"></script>")
     "</body>"
     "</html>")))
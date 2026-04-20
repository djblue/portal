(ns ^:no-doc portal.runtime.index)

(defn html [{:keys [name version host port session-id code-url platform mode]
             :or   {name       "portal"
                    version    "0.64.1"
                    code-url   "main.js"
                    platform   #?(:bb "bb" :clj "jvm" :cljs "node" :cljr "clr" :lpy "py")}}]
  (let [vendor-url "/vendor?url="]
    (str
     "<!DOCTYPE html>"
     "<html lang=\"en\">"
     "<head>"
     "<title>" name " - " platform " - " version "</title>"
     "<meta charset='UTF-8' />"
     "<meta name='viewport' content='width=device-width, initial-scale=1' />"
     "<meta name='theme-color' content='' />"
     "<link rel=\"icon\" href=\"/icon.svg\">"
     (when (= mode :ssr)
       (str
        "<link rel=\"stylesheet\"
              href=\"" vendor-url "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/fontawesome.min.css\" >
        <link rel=\"stylesheet\"
              href=\"" vendor-url "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/solid.min.css\" >"))
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
       (str "<script src=\"" vendor-url "https://cdn.jsdelivr.net/npm/scittle@0.8.31/dist/scittle.js\"
                     type=\"application/javascript\"></script>"
            "<script src=\"" vendor-url "https://unpkg.com/idiomorph@0.7.4\"></script>"
            "<script src=\"" vendor-url "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js\"></script>"
            "<script src=\"" vendor-url "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/languages/clojure.min.js\"></script>"
            "<script> window['highlight.js'] = window.hljs;</script>"
            "<script type=\"application/x-scittle\" src=\"main.cljs\"></script>")
       (str "<script src=\"" code-url "\"></script>"))
     ;; wait.js will ensure headless chrome doesn't exit early
     (when (= mode :test) "<script src=\"wait.js\"></script>")
     "</body>"
     "</html>")))
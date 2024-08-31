(ns ^:no-doc portal.runtime.index
  (:require [portal.colors :as c]))

(def loading-screen
  "
.loading-screen {
  height: 100vh;
  width: 100vw;
  display: flex;
  align-items: center;
  justify-content: center;
}
.loader {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: inline-block;
  position: relative;
  border: 3px solid;
  border-color: #FFF #FFF transparent transparent;
  box-sizing: border-box;
  animation: rotation 1s linear infinite;
}
.loader::after,
.loader::before {
  content: '';
  box-sizing: border-box;
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  margin: auto;
  border: 3px solid;
  border-color: transparent transparent #FF3D00 #FF3D00;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  box-sizing: border-box;
  animation: rotationBack 0.5s linear infinite;
  transform-origin: center center;
}
.loader::before {
  width: 32px;
  height: 32px;
  border-color: #FFF #FFF transparent transparent;
  animation: rotation 1.5s linear infinite;
}
@keyframes rotation {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
@keyframes rotationBack {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(-360deg);
  }
}")

(defn html [{:keys [name version host port session-id code-url platform mode theme]
             :or   {name       "portal"
                    version    "0.58.1"
                    code-url   "main.js"
                    theme      ::c/nord
                    platform   #?(:bb "bb" :clj "jvm" :cljs "node" :cljr "clr")}}]
  (str
   "<!DOCTYPE html>"
   "<html lang=\"en\">"
   "<head>"
   "<title>" (str name " - " platform " - " version) "</title>"
   "<meta charset='UTF-8' />"
   "<meta name='viewport' content='width=device-width, initial-scale=1' />"
   "<meta name='theme-color' content='' />"
   "<link rel=\"icon\" href=\"/icon.svg\">"
   (when (= mode :boot)
     (str "<style>" loading-screen "</style>"))
   "</head>"
   "<body style=\"margin: 0; padding: 0; height: 100vh;"
   (when-let [background (-> theme c/themes ::c/background)]
     (str "background :" background ";"))
   "\">"
   "<div id=\"root\">"
   (when (= mode :boot)
     (str
      "<div class=\"loading-screen\">"
      "<span class=\"loader\"></span>"
      "</div>"))
   "</div>"
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

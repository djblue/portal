(ns portal.runtime.launcher.web
  (:require [portal.resources :as io]
            [portal.runtime :as rt]
            [portal.runtime.transit :as t]))

(defn- str->src [value content-type]
  (let [blob (js/Blob. #js [value] #js {:type content-type})
        url  (or js/window.URL js/window.webkitURL)]
    (.createObjectURL url blob)))

(defn- get-item [k]
  (js/window.localStorage.getItem k))

(defn- set-item [k v]
  (js/window.localStorage.setItem k v))

(defn- remove-item [k]
  (js/localStorage.removeItem k))

(defonce child-window (atom nil))
(defonce code (io/resource "main.js"))
(defonce code-url (str->src code "text/javascript"))

(defn send! [message]
  (js/Promise.
   (fn [resolve _reject]
     (let [body (t/json->edn message)
           f    (get rt/ops (:op body))]
       (f body #(resolve (t/edn->json %)))))))

(defn- index-html []
  (str
   "<head>"
   "<title>portal</title>"
   "<meta charset='UTF-8' />"
   "<meta name='viewport' content='width=device-width, initial-scale=1' />"
   "</head>"
   "<body style=\"margin: 0\">"
   "<div id=\"root\"></div>"
   "<script src=\"" code-url "\"></script>"
   "</body>"))

(defn open [options]
  (swap! rt/state merge {:portal/open? true} options)
  (let [url   (str->src (index-html) "text/html")
        child (js/window.open url "portal", "resizable,scrollbars,status")]
    (set! (.-onunload child)
          (fn []
            (remove-item ":portal/open")))
    (set! (.-onunload js/window)
          (fn []
            (when-not (.-closed child)
              (set-item ":portal/open" (js/Date.now)))))
    (reset! child-window child))
  true)

(defn init []
  (when-let [string (get-item ":portal/open")]
    (if (< (- (js/Date.now) (js/parseInt string)) 5000)
      (open nil)
      (remove-item ":portal/open"))))

(defn close []
  (when-let [child @child-window]
    (reset! child-window nil)
    (.close child)))


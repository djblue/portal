(ns portal.runtime.web.launcher
  (:require [portal.resources :as io]
            [portal.runtime :as rt]
            [portal.runtime.index :as index]
            [portal.runtime.web.client :as c]))

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
(defonce code (io/resource "portal/main.js"))
(defonce code-url (str->src code "text/javascript"))

(defn- not-found [_request done]
  (done {:status :not-found}))

(defn send! [message]
  (js/Promise.
   (fn [resolve _reject]
     (let [session (rt/open-session c/session)
           body    (rt/read message session)
           f       (get rt/ops (:op body) not-found)]
       (binding [rt/*session* session]
         (f body #(resolve (rt/write % session))))))))

(defn- get-session []
  (if (exists? js/PORTAL_SESSION)
    js/PORTAL_SESSION
    (subs js/window.location.search 1)))

(defn- main-js [options]
  (case (:mode options)
    :dev (str js/window.location.origin "/main.js?" (get-session))
    code-url))

(defn open [options]
  (swap! rt/sessions assoc-in [(:session-id c/session) :options] options)
  (let [url   (str->src (index/html :code-url (main-js options)
                                    :platform "web")
                        "text/html")
        child (js/window.open
               url
               "portal"
               (when (:portal.launcher/app options true)
                 "resizable,scrollbars,status"))]
    (set! (.-onunload child)
          (fn []
            (reset! (:value-cache c/session) {})
            (remove-item ":portal/open")))
    (set! (.-onunload js/window)
          (fn []
            (when-not (.-closed child)
              (set-item ":portal/open" (js/Date.now)))))
    (reset! child-window child)
    (reset! rt/request (partial c/request child-window)))
  true)

(defn init []
  (when-let [string (get-item ":portal/open")]
    (if (< (- (js/Date.now) (js/parseInt string)) 5000)
      (open nil)
      (remove-item ":portal/open"))))

(defn clear []
  (c/request child-window {:op :portal.rpc/clear}))

(defn close []
  (when-let [child @child-window]
    (reset! child-window nil)
    (reset! rt/request nil)
    (.close child)))


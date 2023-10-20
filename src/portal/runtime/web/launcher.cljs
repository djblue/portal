(ns ^:no-doc portal.runtime.web.launcher
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

(defonce code (io/inline "portal/main.js"))
(defonce code-url (str->src code "text/javascript"))

(defn- not-found [_request done]
  (done {:status :not-found}))

(defn send! [message]
  (js/Promise.
   (fn [resolve _reject]
     (let [session (merge (rt/get-session (:session-id @c/session)) @c/session)
           body    (rt/read message session)
           id      (:portal.rpc/id body)
           f       (get rt/ops (:op body) not-found)]
       (when (== id 1)
         (swap! c/session rt/reset-session))
       (binding [rt/*session* session]
         (f body #(resolve (rt/write (assoc % :portal.rpc/id id) session))))))))

(defn- get-session []
  (if (exists? js/PORTAL_SESSION)
    js/PORTAL_SESSION
    (subs js/window.location.search 1)))

(defn- main-js [options]
  (case (:mode options)
    :dev (str js/window.location.origin "/main.js?" (get-session))
    code-url))

(defn eval-str [msg]
  (let [response (c/request (assoc msg :op :portal.rpc/eval-str))]
    (if-not (:error response)
      response
      (throw (ex-info (:message response) response)))))

(defn- open-window [options url]
  (let [child (.open js/window
                     url
                     "portal"
                     (when (:app options true)
                       "resizable,scrollbars,status"))]
    (reset! c/connection child)
    (set! (.-onunload child)
          (fn []
            (remove-item ":portal/open")))
    (set! (.-onunload js/window)
          (fn []
            (when-not (.-closed child)
              (set-item ":portal/open" (js/Date.now)))))
    (when-let [f (:on-load options)]
      (set! (.-onload child)
            (fn [] (f))))))

(defn- open-iframe [{:keys [iframe-parent]} url]
  (let [iframe
        (doto (.createElement js/document "iframe")
          (.setAttribute "src" url)
          (.setAttribute "style" "width: 100%; height: 100%; border: 0"))]
    (.appendChild iframe-parent iframe)
    (-> iframe .-contentWindow .-window .-opener (set! js/window))
    (reset! c/connection (.-contentWindow iframe))))

(defn open [options]
  (swap! rt/sessions assoc-in [(:session-id @c/session) :options] options)
  (swap! c/session rt/open-session)
  (let [options (merge options @rt/default-options)
        url     (str->src (index/html {:code-url (main-js options)
                                       :platform "web"})
                          "text/html")]
    (case (:launcher options)
      :iframe (open-iframe options url)
      (open-window options url)))
  (c/make-atom (:session-id @c/session)))

(defn init [options]
  (when-let [string (get-item ":portal/open")]
    (if (< (- (js/Date.now) (js/parseInt string)) 5000)
      (open options)
      (remove-item ":portal/open"))))

(defn clear []
  (c/request {:op :portal.rpc/clear}))

(defn close []
  (when-let [child @c/connection]
    (reset! c/connection nil)
    (.close child)))

(reset! rt/request c/request)

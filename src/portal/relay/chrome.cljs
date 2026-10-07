(ns portal.relay.chrome
  (:require
   [portal.shadow.remote :as remote]))

(defn- chrome->remote
  "Takes a chrome message and relays a message 
   to the remote portal instance"
  [v _sender _send-response]
  (let [{:keys [content-type body portal-relay?]} (js->clj v :keywordize-keys true)
        port (remote/get-port)
        host remote/host]
    (when portal-relay?
      (js/fetch
       (str "http://" host ":" port "/submit")
       #js {:method "POST"
            :headers #js {"content-type" content-type}
            :body body}))
    ;; Return explicit nil, a promise result may result
    ;; the handler awaiting a response
    nil))

(defn start-chrome->remote-relay!
  "Listen for Chrome messages, forward them to 
   the configured HTTP remote"
  []
  (js/chrome.runtime.onMessage.addListener chrome->remote))

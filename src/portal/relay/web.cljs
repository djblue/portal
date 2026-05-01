(ns portal.relay.web)

(defn- window->chrome
  "Takes a window message, and produces a chrome
   message"
  [^js event]
  (let [message (js->clj (.-data event) :keywordize-keys true)]
    (when (:portal-relay? message)
      (js/chrome.runtime.sendMessage message))))

(defn start-window->chrome-relay!
  "Start js window relay listener"
  []
  (js/window.addEventListener "message" window->chrome))

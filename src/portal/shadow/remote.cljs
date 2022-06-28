(ns portal.shadow.remote
  (:require [portal.client.web :as client]))

(goog-define port 0)

(defn get-port
  "Get portal server port."
  {:added "0.28.0"}
  []
  (if-not (zero? port)
    port
    (let [error (js/Error.
                 (str "Portal server port is missing. "
                      "Did you add the portal.shadow.remote/hook to :build-hooks in shadow-cljs.edn? \n"
                      "See https://shadow-cljs.github.io/docs/UsersGuide.html#build-hooks for more info."))]
      (.error js/console error)
      (throw error))))

(defn submit
  "Tap target function.

  Usage:
    (add-tap portal.shadow/submit)
    (remove-tap portal.shadow/submit)"
  {:added "0.28.0"
   :see-also ["portal.api/submit"
              "portal.client.web/submit"]}
  ([value]
   (submit {:encoding :edn} value))
  ([option value]
   (client/submit (assoc option :port (get-port)) value)))

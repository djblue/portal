(ns user
  (:require [com.brunobonacci.mulog :as mu]
            [com.brunobonacci.mulog.buffer :as rb]
            [portal.api :as p]))

;; μ/log publisher
;; Based on https://github.com/BrunoBonacci/mulog/blob/c417d67/mulog-core/src/com/brunobonacci/mulog/publisher.clj#L42-L66

(deftype TapPublisher [buffer transform]
  com.brunobonacci.mulog.publisher.PPublisher
  (agent-buffer [_]
    buffer)

  (publish-delay [_]
    200)

  (publish [_ buffer]
    (doseq [item (transform (map second (rb/items buffer)))]
      (tap> item))
    (rb/clear buffer)))

(defn tap-publisher
  [{:keys [transform] :as _config}]
  (TapPublisher. (rb/agent-buffer 10000) (or transform identity)))

;;; main

(comment
  (def p (p/open))
  (add-tap #'p/submit)

  (tap> (ns-publics *ns*))

  (def pub! (mu/start-publisher!
              {:type :custom, :fqn-function "user/tap-publisher"}))

  (mu/log ::my-event ::ns (ns-publics *ns*))
  ,)

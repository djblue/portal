(ns ^:no-doc portal.nrepl
  (:require [clojure.datafy :as d]
            [clojure.test :as test]
            [nrepl.middleware :refer [set-descriptor!]]
            [nrepl.middleware.caught :as caught]
            [nrepl.middleware.print :as print]
            [nrepl.transport :as transport]
            [portal.api :as p])
  (:import [java.util Date]
           [nrepl.transport Transport]))

; fork of https://github.com/DaveWM/nrepl-rebl/blob/master/src/nrepl_rebl/core.clj

(deftype Print [string] Object (toString [_] string))

(defmethod print-method Print [v ^java.io.Writer w]
  (.write w ^String (str v)))

(defn- get-result [response]
  (cond
    (contains? response :nrepl.middleware.caught/throwable)
    {:level  :error
     :file   "*repl*"
     :line   1
     :column 1
     :result (d/datafy (:nrepl.middleware.caught/throwable response))}

    (and (contains? response :value)
         (not= (:value response) ::p/ignore))
    {:level  :info
     :file   "*repl*"
     :line   1
     :column 1
     :result (if-not (:printed-value response)
               (:value response)
               (->Print (:value response)))}))

(defn- shadow-cljs?
  "Determine if the current message was handled by shadow-cljs."
  [msg]
  (try
    (some?
     (get
      @(:session msg)
      (requiring-resolve
       'shadow.cljs.devtools.server.nrepl-impl/*repl-state*)))
    (catch Exception _ false)))

(defrecord PortalTransport [transport handler-msg]
  Transport
  (recv [_this timeout]
    (transport/recv transport timeout))
  (send [_this  msg]
    (transport/send transport msg)
    (when (seq (p/sessions))
      (when-let [out (:out msg)]
        (swap! (:stdio handler-msg) conj {:tag :out :val out}))
      (when-let [err (:err msg)]
        (swap! (:stdio handler-msg) conj {:tag :err :val err}))
      (when-let [result (get-result msg)]
        (-> result
            (merge
             (select-keys handler-msg [:ns :file :column :line :code])
             (when-let [report (-> handler-msg :report deref not-empty)]
               {:report report})
             (when-let [stdio (-> handler-msg :stdio deref not-empty)]
               {:stdio stdio}))
            (update :ns (fnil symbol 'user))
            (assoc :time     (Date.)
                   :ms       (quot (- (System/nanoTime) (:start handler-msg)) 1000000)
                   :runtime  (if (shadow-cljs? handler-msg) :cljs :clj))
            (with-meta {:portal.viewer/for
                        {:code :portal.viewer/code
                         :time :portal.viewer/relative-time}
                        :portal.viewer/code {:language :clojure}})
            p/submit)))
    transport))

(defn- wrap-portal* [handler msg]
  (let [report         (atom [])
        test-report    test/report
        portal-report  (fn [value]
                         (swap! report conj value)
                         (test-report value))]
    (handler
     (cond-> msg
       (= (:op msg) "eval")
       (-> (update :transport
                   ->PortalTransport
                   (assoc msg
                          :report report
                          :stdio  (atom [])
                          :start  (System/nanoTime)))
           (update :session
                   (fn [session]
                     (swap! session assoc
                            #'test/report portal-report)
                     session)))))))

(defn wrap-portal [handler] (partial #'wrap-portal* handler))

(defn- get-shadow-middleware []
  (try
    [(requiring-resolve 'shadow.cljs.devtools.server.nrepl/middleware)]
    (catch Exception _)))

(set-descriptor! #'wrap-portal
                 {:requires #{"clone"
                              #'print/wrap-print
                              #'caught/wrap-caught}
                  :expects (into #{"eval"} (get-shadow-middleware))
                  :handles {}})

(ns ^:no-doc portal.nrepl
  (:require [clojure.datafy :as d]
            [clojure.main :as main]
            [clojure.test :as test]
            [nrepl.middleware :refer [set-descriptor!]]
            [nrepl.middleware.caught :as caught]
            [nrepl.middleware.print :as print]
            [nrepl.misc :refer (response-for)]
            [nrepl.transport :as transport]
            [portal.api :as p])
  (:import [java.util Date]
           [nrepl.transport Transport]))

; fork of https://github.com/DaveWM/nrepl-rebl/blob/master/src/nrepl_rebl/core.clj

(def ^:dynamic *portal-ns* nil)
(def ^:dynamic *portal-session* nil)

(deftype Print [string] Object (toString [_] string))

(defmethod print-method Print [v ^java.io.Writer w]
  (.write w ^String (str v)))

(defn- get-result [response]
  (cond
    (contains? response ::caught/throwable)
    {:level  :error
     :line   1
     :column 1
     :ns (:ns response)
     :result (d/datafy (::caught/throwable response))}

    (and (contains? response :value)
         (not= (:value response) ::p/ignore))
    {:level  :info
     :line   1
     :column 1
     :ns (:ns response)
     :result (if-not (:printed-value response)
               (:value response)
               (->Print (:value response)))}))

(defn- in-portal? [msg]
  (some? (get @(:session msg) #'*portal-session*)))

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
    (when (and (seq (p/sessions)) (:file handler-msg))
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
                   :runtime  (cond
                               (shadow-cljs? handler-msg) :cljs
                               (in-portal? handler-msg)   :portal
                               :else                      :clj))
            (with-meta {::eval true
                        :portal.viewer/for
                        {:code :portal.viewer/code
                         :time :portal.viewer/relative-time}
                        :portal.viewer/code {:language :clojure}})
            tap>)))
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

(defn- wrap-repl* [handler {:keys [op session transport] :as msg}]
  (when (and (= "eval" op)
             (not (contains? @session #'*portal-session*)))
    (swap! session assoc
           #'*portal-session* nil
           #'p/*nrepl-init*   (fn [portal]
                                (set! *portal-session* portal)
                                (println "To quit, type:" :cljs/quit)
                                [:repl portal])))
  (if-let [portal (and (= op "eval")
                       (get @session #'*portal-session*))]
    (->> (if-not (contains? (into #{} (p/sessions)) portal)
           (do (swap! session dissoc #'*portal-session* #'*portal-ns*)
               {:value       :cljs/quit
                :status      :done
                ::print/keys #{:value}})
           (try
             (let [{:keys [value] :as response}
                   (p/eval-str
                    portal
                    (:code msg)
                    (-> {:ns (get @session #'*portal-ns*)}
                        (merge  msg)
                        (select-keys  [:ns :file :line :column])
                        (assoc  :verbose true)))]
               (when-let [namespace (:ns response)]
                 (swap! session assoc #'*portal-ns* namespace))
               (when (= value :cljs/quit)
                 (swap! session dissoc #'*portal-session* #'*portal-ns*))
               (merge
                response
                {:status      :done
                 ::print/keys #{:value}}))
             (catch Exception e
               (swap! session assoc #'*e e)
               {::caught/throwable e
                :status            :eval-error
                :ex                (str (class e))
                :root-ex           (str (class (main/root-cause e)))})))
         (response-for msg)
         (transport/send transport))
    (handler msg)))

(defn wrap-repl [handler] (partial #'wrap-repl* handler))

(set-descriptor! #'wrap-repl
                 {:requires #{"clone"
                              #'print/wrap-print
                              #'caught/wrap-caught}
                  :expects (into #{"eval"})
                  :handles {}})

(def ^:private id-gen (atom 0))
(def ^:private values (atom (sorted-map)))
(defn- next-id [] (swap! id-gen inc))

(defn- ->value [id]
  (let [value (get @values id)]
    (swap! values dissoc id)
    value))

(p/register! #'->value)

(defn- ->id [value]
  (let [id (next-id)]
    (swap!
     values
     #(cond-> %
        (> (count %) 32) (dissoc (ffirst %))
        :always          (assoc id value)))
    id))

(defrecord NotebookTransport [transport]
  Transport
  (recv [_this timeout]
    (transport/recv transport timeout))
  (send [_this  message]
    (if-not (contains? message :value)
      (transport/send transport message)
      (transport/send
       transport
       (update message :value
               #(with-meta
                  (list `->value (->id %))
                  (-> (p/start {})
                      (select-keys  [:port :host])
                      (assoc ::eval true :protocol "ws:"))))))
    transport))

(defn- wrap-notebook* [handler {:keys [op] :as message}]
  (handler
   (cond-> message
     (and (= op "eval")
          (get-in message [:nrepl.middleware.eval/env :calva-notebook]))
     (update :transport ->NotebookTransport))))

(defn wrap-notebook [handler] (partial #'wrap-notebook* handler))

(set-descriptor! #'wrap-notebook
                 {:requires #{"clone"
                              #'print/wrap-print
                              #'caught/wrap-caught}
                  :expects #{"eval"}
                  :handles {}})

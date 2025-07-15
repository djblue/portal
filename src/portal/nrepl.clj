(ns portal.nrepl
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

(def ^:no-doc ^:dynamic *portal-ns* nil)
(def ^:no-doc ^:dynamic *portal-session* nil)

(deftype ^:no-doc Print [string] Object (toString [_] string))

(defmethod print-method Print [v ^java.io.Writer w]
  (.write w ^String (str v)))

(defn- get-result [response]
  (cond
    (contains? response ::caught/throwable)
    {:level  :error
     :line   1
     :column 1
     :ns (:ns response)
     :result (let [error (:error (ex-data (::caught/throwable response)))]
               (if (and (map? error)
                        (contains? error :via))
                 error
                 (d/datafy (::caught/throwable response))))}

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

(defn- read-cursive-file-meta [{:keys [code] :as msg}]
  (try
    (if (contains? msg :file)
      msg
      (let [m (meta (read-string code))]
        (if-not (:clojure.core/eval-file m)
          msg
          (assoc msg
                 :line   (:line m 1)
                 :column (:column m 1)
                 :file   (:clojure.core/eval-file m)))))
    (catch Exception _ msg)))

(defrecord ^:no-doc PortalTransport [transport handler-msg]
  Transport
  (recv [_this timeout]
    (transport/recv transport timeout))
  (send [_this  msg]
    (transport/send transport msg)
    (try
      (let [handler-msg (read-cursive-file-meta handler-msg)]
        (when (and (seq (p/sessions)) (:file handler-msg))
          (when-let [out (:out msg)]
            (swap! (:stdio handler-msg) conj {:tag :out :val out}))
          (when-let [err (:err msg)]
            (swap! (:stdio handler-msg) conj {:tag :err :val err}))
          (when-let [result (get-result msg)]
            (-> result
                (merge
                 (select-keys handler-msg [:ns :file :column :line :code])
                 (when (= "load-file" (:op handler-msg))
                   {:code (:file handler-msg)
                    :file (:file-path handler-msg)})
                 (when-let [report (-> handler-msg :report deref not-empty)]
                   {:report report})
                 (when-let [stdio (-> handler-msg :stdio deref not-empty)]
                   {:stdio stdio}))
                (update :ns (fnil symbol 'user))
                (assoc :time     (:time handler-msg)
                       :ms       (quot (- (System/nanoTime) (:start handler-msg)) 1000000)
                       :runtime  (cond
                                   (shadow-cljs? handler-msg) :cljs
                                   (in-portal? handler-msg)   :portal
                                   :else                      :clj))
                (with-meta {::eval true
                            :portal.viewer/for
                            {:code :portal.viewer/code
                             :time :portal.viewer/relative-time
                             :ms   :portal.viewer/duration-ms}
                            :portal.viewer/code {:language :clojure}})
                tap>))))
      (catch Exception _))
    transport))

(def ^:private ^:dynamic *test-report* nil)

(defmulti ^:dynamic report (constantly :default))

(defn- add-method [^clojure.lang.MultiFn multifn dispatch-val f]
  (.addMethod multifn dispatch-val f))

(doseq [[dispatch-value f] (methods test/report)]
  (add-method report dispatch-value f))

(defmethod report :default [message]
  (when *test-report*
    (swap! *test-report* conj message))
  (when-let [f (get-method report (:type message))]
    (f message)))

(defn- wrap-portal* [handler msg]
  (let [test-report (atom [])]
    (handler
     (cond-> msg
       (#{"eval" "load-file"} (:op msg))
       (-> (update :transport
                   ->PortalTransport
                   (assoc msg
                          :report test-report
                          :stdio  (atom [])
                          :start  (System/nanoTime)
                          :time   (Date.)))
           (update :session
                   (fn [session]
                     (swap! session assoc
                            #'test/report report
                            #'*test-report* test-report)
                     session)))))))

(defn wrap-portal
  "nREPL middleware for inspecting `eval` and `load-file` ops for the following to `tap>`:

   - evaluation result / exceptions
   - stdout / stderr
   - clojure.test/report output

  Data produced via this middleware will contain `{:portal.nrepl/eval true}` as
  metadata and leverages the following viewers:

  - :portal.viewer/code
  - :portal.viewer/duration-ms
  - :portal.viewer/ex
  - :portal.viewer/log
  - :portal.viewer/prepl
  - :portal.viewer/relative-time
  - :portal.viewer/test-report"
  [handler]
  (partial #'wrap-portal* handler))

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
  (if-let [portal (and (#{"eval" "load-file"} op)
                       (get @session #'*portal-session*))]
    (->> (if-not (contains? (into #{:all} (p/sessions)) portal)
           (do (swap! session dissoc #'*portal-session* #'*portal-ns*)
               {:value       :cljs/quit
                :status      :done
                ::print/keys #{:value}})
           (try
             (let [[code file]
                   (if (= "load-file" (:op msg))
                     [(:file msg) (:file-path msg)]
                     [(:code msg) (:file msg)])
                   {:keys [value] :as response}
                   (p/eval-str
                    portal
                    code
                    (-> {:ns (get @session #'*portal-ns*)}
                        (merge msg)
                        (select-keys  [:ns :line :column])
                        (assoc :file file
                               :verbose true
                               :context (case op "eval" :expr "load-file" :statement)
                               :re-render (= op "load-file"))))]
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
                :status            [:done :eval-error]
                :ex                (str (class e))
                :root-ex           (str (class (main/root-cause e)))
                :causes            (if-let [via (get-in (ex-data e) [:error :via])]
                                     (for [{:keys [type message at]} via]
                                       {:class      type
                                        :message    message
                                        :stacktrace at})
                                     (for [ex (take-while some? (iterate ex-cause e))]
                                       {:class      (str (class ex))
                                        :message    (ex-message ex)
                                        :stacktrace []}))})))
         (response-for msg)
         (transport/send transport))
    (handler msg)))

(defn wrap-repl
  "nREPL middleware for exposing `portal.api/eval-str` directly to editors.
  This enabled sending code directly from your editor to the portal runtime for
  evaluation."
  {:see-also ["portal.api/repl"]}
  [handler]
  (partial #'wrap-repl* handler))

(set-descriptor! #'wrap-repl
                 {:requires #{"clone"
                              #'wrap-portal
                              #'print/wrap-print
                              #'caught/wrap-caught}
                  :expects (into #{"eval"} (get-shadow-middleware))
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

(defn- link-value [value]
  (with-meta
    (list `->value (->id value))
    (-> (p/start {})
        (select-keys  [:port :host])
        (assoc ::eval true :protocol "ws:"))))

(defn- intercept-value [message]
  (cond-> message
    (contains? message ::caught/throwable)
    (assoc :ex
           (binding [*print-meta* true]
             (pr-str
              (link-value
               (d/datafy (::caught/throwable message))))))

    (contains? message :value)
    (update :value link-value)))

(defrecord ^:no-doc NotebookTransport [transport]
  Transport
  (recv [_this timeout]
    (transport/recv transport timeout))
  (send [_this  message]
    (transport/send transport (intercept-value message))
    transport))

(defn- wrap-notebook* [handler {:keys [op] :as message}]
  (handler
   (cond-> message
     (and (= op "eval")
          (get-in message [:nrepl.middleware.eval/env :calva-notebook]))
     (update :transport ->NotebookTransport))))

(defn wrap-notebook
  "nREPL middleware for use with [Calva Notebooks](https://calva.io/notebooks/)
  to support first class values for datafy / nav and all other features that
  require a runtime connected portal."
  [handler]
  (partial #'wrap-notebook* handler))

(set-descriptor! #'wrap-notebook
                 {:requires #{"clone"
                              #'print/wrap-print
                              #'caught/wrap-caught}
                  :expects #{"eval"}
                  :handles {}})

(def middleware
  "All of portal's nREPL middleware."
  [`wrap-notebook
   `wrap-repl
   `wrap-portal])
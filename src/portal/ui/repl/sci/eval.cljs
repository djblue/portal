(ns ^:no-doc portal.ui.repl.sci.eval
  (:refer-clojure :exclude [Throwable->map])
  (:require [clojure.string :as str]
            [portal.ui.cljs :as cljs]
            [portal.ui.load :as load]
            [portal.ui.repl.sci.libs :as libs]
            [sci.core :as sci]))

(sci/alter-var-root sci/print-fn (constantly *print-fn*))
(sci/alter-var-root sci/print-err-fn (constantly *print-err-fn*))

(defn- find-ns* [ctx ns-sym]
  (sci/eval-form ctx (list 'clojure.core/the-ns (list 'quote ns-sym))))

(defonce ctx (atom nil))

(defn- ns->file [ns]
  (str (str/join "/" (str/split (munge (name ns)) #"\.")) ".cljs"))

(def ^:private ->aliases
  '{clojure.core cljs.core})

(defn- ex-trace [ex]
  (when-let [stacktrace (sci/stacktrace ex)]
    (into
     []
     (for [{:keys [_column file line ns] name* :name} stacktrace
           :let [ns (->aliases ns ns)]]
       [(symbol (str ns)
                (name (or name* (gensym "eval"))))
        'invoke
        (or file
            (ns->file ns))
        (or line 1)]))))

(def ^:private ex-type cljs.core/ExceptionInfo)

(defn- ->class [ex]
  (if (= ex-type (type ex))
    'cljs.core/ExceptionInfo
    (symbol (.-name (type ex)))))

(defn- ex-chain [ex]
  (reverse (take-while some? (iterate ex-cause ex))))

(defn- Throwable->map [ex]
  (let [[ex :as chain] (ex-chain ex)]
    {:runtime :portal
     :cause   (ex-message ex)
     :data    (ex-data ex)
     :via     (mapv
               (fn [ex]
                 (merge
                  {:type    (->class ex)
                   :data    (ex-data ex)
                   :message (ex-message ex)}
                  (when-let [at (first (ex-trace ex))]
                    {:at at})))
               chain)
     :trace   (vec (mapcat ex-trace chain))}))

(defn eval-string [msg]
  (try
    (let [ctx    @ctx
          sci-ns (if-let [ns (some-> msg :ns symbol)]
                   (try (find-ns* ctx ns)
                        (catch :default _
                          (sci/create-ns ns nil)))
                   (sci/create-ns 'cljs.user nil))
          reader (sci/source-reader (:code msg))
          stdio  (atom [])
          out-fn (fn [val] (swap! stdio conj val))]
      (when-let [n (:line msg)]   (set! (.-line reader) n))
      (when-let [n (:column msg)] (set! (.-column reader) n))
      (sci/with-bindings
        {sci/*1 *1
         sci/*2 *2
         sci/*3 *3
         sci/*e *e
         sci/ns sci-ns
         sci/print-newline true
         sci/print-fn      #(out-fn {:tag :out :val %})
         sci/print-err-fn  #(out-fn {:tag :err :val %})
         sci/file (:file msg)}
        (cond->
         {:value (loop [last-val nil]
                   (let [[form _s] (sci/parse-next+string
                                    ctx reader
                                    {:read-cond :allow})]
                     (if (= ::sci/eof form)
                       last-val
                       (let [value (sci/eval-form ctx form)]
                         (set! *3 *2)
                         (set! *2 *1)
                         (set! *1 value)
                         (recur value)))))
          :ns    (str @sci/ns)}

          (seq @stdio) (assoc :stdio @stdio))))
    (catch :default e
      (throw (ex-info "eval-error" (Throwable->map e))))))

(defn- ns->path [ns]
  (-> (name ns)
      (str/replace  #"\." "/")
      (str/replace  #"\-" "_")))

(defn loan-fn [{:keys [namespace]}]
  (if (string? namespace)
    (sci/add-js-lib! @ctx namespace (load/node-require namespace))
    (load/load-fn-sync {:name namespace :path (ns->path namespace)})))

(defn init []
  (load/load-require-cache libs/js-libs)
  (reset! ctx (libs/init {:load-fn loan-fn :features #{:cljs :portal}})))

(reset! cljs/init-fn init)
(reset! cljs/eval-fn eval-string)

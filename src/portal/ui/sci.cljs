(ns ^:no-doc portal.ui.sci
  (:require [clojure.string :as str]
            [portal.ui.cljs :as cljs]
            [portal.ui.load :as load]
            [portal.ui.sci.libs :as libs]
            [sci.core :as sci]))

(sci/alter-var-root sci/print-fn (constantly *print-fn*))

(defn- find-ns* [ctx ns-sym]
  (sci/eval-form ctx (list 'clojure.core/the-ns (list 'quote ns-sym))))

(defonce ctx (atom nil))

(defn- ex->data
  [ex phase]
  (with-meta
    #_{:clj-kondo/ignore [:unresolved-symbol]}
    (assoc (Throwable->map ex) :phase phase :runtime :portal)
    (ex-data ex)))

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
        (loop []
          (when
           (try
             (let [[form s] (sci/parse-next+string ctx reader {:read-cond :allow})]
               (try
                 (when-not (= ::sci/eof form)
                   (let [start (.now (.-performance js/window))
                         ret (sci/eval-form ctx form)
                         ms (quot (- (.now (.-performance js/window)) start) 1)]
                     (set! *3 *2)
                     (set! *2 *1)
                     (set! *1 ret)
                     (out-fn {:tag :ret
                              :val (if (instance? js/Error ret)
                                     #_{:clj-kondo/ignore [:unresolved-symbol]}
                                     (assoc (Throwable->map ret) :runtime :portal)
                                     ret)
                              :ns (str @sci/ns)
                              :ms ms
                              :form s})
                     true))
                 (catch :default ex
                   (when (:verbose msg) (.error js/console ex))
                   (set! *e ex)
                   (out-fn {:tag :ret :val (ex->data ex (or (some-> ex ex-data :phase keyword) :execution))
                            :ns (str @sci/ns) :form s
                            :exception true})
                   true)))
             (catch :default ex
               (when (:verbose msg) (.error js/console ex))
               (set! *e ex)
               (out-fn {:tag :ret :val (ex->data ex :read-source)
                        :ns (str @sci/ns)
                        :exception true})
               true))
            (recur)))
        (if-not (:await msg)
          @stdio
          (let [return        @stdio
                {:keys [val] :as result} (last return)]
            (-> (.resolve js/Promise val)
                (.then #(conj (pop return) (assoc result :val %)))
                (.catch #(conj (pop return) (assoc result :val % :exception true))))))))
    (catch :default e
      (let [sci-error? (isa? (:type (ex-data e)) :sci/error)]
        (throw (if sci-error?
                 (or (ex-cause e) e)
                 e))))))

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

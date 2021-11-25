(ns portal.console
  #?(:clj (:require [clojure.java.io :as io]))
  #?(:cljs (:require-macros portal.console)))

(defn now []
  #?(:clj (java.util.Date.) :cljs (js/Date.)))

(defn run [f]
  (try
    [nil (f)]
    (catch #?(:clj Exception :cljs :default) ex#
      [:throw ex#])))

(defn runtime []
  #?(:bb :bb :clj :clj :cljs :cljs))

#?(:clj
   (defn get-file [env file]
     (if (:ns env) ;; cljs target
       (if-let [classpath-file (io/resource file)]
         (.toString classpath-file)
         file)
       *file*)))

(defn capture [level form expr env]
  (let [{:keys [line column file]} (meta form)]
    `(let [[flow# result#] (run (fn [] ~expr))]
       (tap>
        {:form     (quote ~expr)
         :level    (if (= flow# :throw) :fatal ~level)
         :result   result#
         :ns       (quote ~(symbol (str *ns*)))
         :file     ~#?(:clj (get-file env file) :cljs nil)
         :line     ~line
         :column   ~column
         :time     (now)
         :runtime  (runtime)})
       (if (= flow# :throw) (throw result#) result#))))

(defmacro log   [expr] (capture :info &form expr &env))

(defmacro trace [expr] (capture :trace &form expr &env))
(defmacro debug [expr] (capture :debug &form expr &env))
(defmacro info  [expr] (capture :info  &form expr &env))
(defmacro warn  [expr] (capture :warn  &form expr &env))
(defmacro error [expr] (capture :error &form expr &env))
(defmacro fatal [expr] (capture :fatal &form expr &env))

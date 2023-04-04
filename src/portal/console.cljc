(ns portal.console
  #?(:clj    (:require [clojure.java.io :as io])
     :portal (:import)
     :cljs   (:require-macros portal.console)))

(defn ^:no-doc now []
  #?(:clj (java.util.Date.) :cljs (js/Date.) :cljr (DateTime/Now)))

(defn ^:no-doc run [f]
  (try
    [nil (f)]
    (catch #?(:clj Exception :cljs :default :cljr Exception) ex#
      [:throw ex#])))

(defn ^:no-doc runtime []
  #?(:portal :portal :bb :bb :clj :clj :cljs :cljs :cljr :cljr))

#?(:clj
   (defn ^:no-doc get-file [env file]
     (if (:ns env) ;; cljs target
       (if-let [classpath-file (io/resource file)]
         (.getPath (io/file classpath-file))
         file)
       *file*)))

#_{:clj-kondo/ignore #?(:clj [] :cljs [:unused-binding])}
(defn ^:no-doc capture [level form expr env]
  (let [{:keys [line column file]} (meta form)]
    `(let [[flow# result#] (run (fn [] ~expr))]
       (tap>
        {:form     (quote ~expr)
         :level    (if (= flow# :throw) :fatal ~level)
         :result   result#
         :ns       (quote ~(symbol (str *ns*)))
         :file     ~#?(:clj (get-file env file) :portal *file* :cljs nil :cljr *file*)
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

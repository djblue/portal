(ns portal.runtime.edn
  (:refer-clojure :exclude [read-string])
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.walk :as w]
            [portal.runtime.cson :as cson]))

;; Discard metadata on tagged-literals to improve success rate of read-string.
;; Consider using a different type in the future.
#?(:cljs
   (extend-type cljs.core/TaggedLiteral
     IMeta     (-meta [_this] nil)
     IWithMeta (-with-meta [this _m] this)))

(defn- with-file [v]
  #?(:cljs    v
     :default (cond-> v
                *file*
                (vary-meta assoc ::file *file*))))

(defn- ->var [var-symbol]
  (with-file (cson/tagged-value "portal/var" var-symbol)))

(defn read-string [edn-string]
  (w/postwalk
   (fn [v]
     (if-let [file (::file (meta v))]
       (-> v
           (vary-meta dissoc ::file)
           (vary-meta assoc :file file))
       v))
   (edn/read-string
    {:readers {'portal/var ->var
               'portal/bin cson/base64-decode}
     :default tagged-literal}
    (str/replace edn-string #"#'" "#portal/var "))))

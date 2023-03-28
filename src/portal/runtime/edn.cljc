(ns ^:no-doc portal.runtime.edn
  (:refer-clojure :exclude [read-string])
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [portal.runtime.cson :as cson]))

;; Discard metadata on tagged-literals to improve success rate of read-string.
;; Consider using a different type in the future.
#?(:cljs
   (extend-type cljs.core/TaggedLiteral
     IMeta     (-meta [_this] nil)
     IWithMeta (-with-meta [this _m] this)))

(defn- ->var [var-symbol]
  (cson/tagged-value "portal/var" var-symbol))

(defn read-string
  ([edn-string]
   (read-string {} edn-string))
  ([{:keys [readers]} edn-string]
   (edn/read-string
    {:readers (merge
               {'portal/var ->var
                'portal/bin cson/base64-decode}
               readers)
     :default tagged-literal}
    (str/replace edn-string #"#'" "#portal/var "))))

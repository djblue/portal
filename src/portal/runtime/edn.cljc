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

(defn- escape-var
  "Allows parsing edn that contains vars."
  [edn-string]
  (str/replace edn-string #"#'" "#portal/var "))

(defn- ->regex [re-string]
  (cson/tagged-value "portal/re" re-string))

(defn- escape-regex
  "Allows parsing edn that contains regular expressions."
  [edn-string]
  (-> edn-string
      (str/replace #"#\"" "#portal/re \"")
      ;; \Q and \E produce invalid escape characters
      (str/replace #"[^\\]\\(Q|E)" "\\\\$1")))

(defn read-string
  ([edn-string]
   (read-string {} edn-string))
  ([{:keys [readers]} edn-string]
   (edn/read-string
    {:readers (merge
               {'portal/var ->var
                'portal/re ->regex
                'portal/bin cson/base64-decode}
               readers)
     :default tagged-literal}
    (-> edn-string escape-var escape-regex))))

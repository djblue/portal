(ns ^:no-doc portal.runtime.edn
  (:refer-clojure :exclude [read-string])
  (:require #?(:org.babashka/nbb [clojure.core]
               :cljs [cljs.tools.reader.impl.commons :as commons])
            #?(:org.babashka/nbb [clojure.core]
               :cljs [cljs.tools.reader.impl.utils :refer [numeric?]])
            [clojure.edn :as edn]
            [clojure.string :as str]
            [portal.runtime.cson :as cson]))

;; Discard metadata on tagged-literals to improve success rate of read-string.
;; Consider using a different type in the future.
#?(:org.babashka/nbb nil
   :cljs
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

#?(:org.babashka/nbb :nil
   :cljs
   (defn parse-symbol
     "Parses a string into a vector of the namespace and symbol
     Fork: https://github.com/clojure/tools.reader/blob/master/src/main/cljs/cljs/tools/reader/impl/commons.cljs#L97-L118"
     [token]
     (when-not (or (identical? "" token)
                   (true? (.test #":$" token))
                   (true? (.test #"^::" token)))
       (let [ns-idx (.indexOf token "/")
             ns (when (pos? ns-idx)
                  (subs token 0 ns-idx))]
         (if-not (nil? ns)
           (let [ns-idx (inc ns-idx)]
             (when-not (== ns-idx (count token))
               (let [sym (subs token ns-idx)]
                 (when (and (not (numeric? (nth sym 0)))
                            (not (identical? "" sym))
                            (false? (.test #":$" ns)))
                   [ns sym]))))
           [nil token])))))

(defn read-string
  ([edn-string]
   (read-string {} edn-string))
  ([{:keys [readers]} edn-string]
   (with-redefs  #?(:org.babashka/nbb [] :cljs [commons/parse-symbol parse-symbol] :default [])
     (edn/read-string
      {:readers (merge
                 {'portal/var ->var
                  'portal/re ->regex
                  'portal/bin cson/base64-decode}
                 readers)
       :default tagged-literal}
      (-> edn-string escape-var escape-regex)))))
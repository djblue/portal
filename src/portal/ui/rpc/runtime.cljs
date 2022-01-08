(ns ^:no-doc portal.ui.rpc.runtime
  (:refer-clojure :exclude [deref pr-str])
  (:require [portal.runtime.cson :as cson]
            [reagent.core :as r]))

(defonce current-values (r/atom {}))

(defn tag [value] (:tag (.-object value)))

(defn rep [value] (:rep (.-object value)))

(defn deref [this]
  (-> ((.-runtime this) 'clojure.core/deref this)
      (.then #(swap! current-values
                     assoc-in [this 'clojure.core/deref] %))))

(defn pr-str [this]
  (-> ((.-runtime this) 'clojure.core/pr-str this)
      (.then #(swap! current-values
                     assoc-in [this 'clojure.core/pr-str] %))))

(defn- runtime-deref [this]
  (when (= ::not-found (get-in @current-values [this 'clojure.core/deref] ::not-found))
    (deref this))
  (get-in @current-values [this 'clojure.core/deref]))

(defn- runtime-print [this writer _opts]
  (when (= ::not-found (get-in @current-values [this 'clojure.core/pr-str] ::not-found))
    (pr-str this))
  (-write writer
          (or (get-in @current-values [this 'clojure.core/pr-str])
              (if (not= (tag this) :var)
                "loading"
                (str "#'" (rep this))))))

(defn- runtime-to-json [this]
  (let [object (.-object this)]
    (if-let [to-object (:to-object cson/*options*)]
      (to-object this :runtime-object nil)
      (cson/tag "ref" (:id object)))))

(defn- runtime-meta [this] (:meta (.-object this)))

(defprotocol Runtime)

(deftype RuntimeObject [runtime object]
  Runtime
  cson/ToJson (-to-json [this] (runtime-to-json this))
  IMeta       (-meta    [this] (runtime-meta this))
  IHash       (-hash    [_]    (hash object))
  IWithMeta
  (-with-meta [_this m]
    (RuntimeObject.
     runtime
     (assoc object :meta m)))
  IPrintWithWriter
  (-pr-writer [this writer _opts]
    (runtime-print this writer _opts)))

(deftype RuntimeAtom [runtime object]
  Runtime
  cson/ToJson (-to-json [this] (runtime-to-json this))
  IMeta       (-meta    [this] (runtime-meta this))
  IDeref      (-deref   [this] (runtime-deref this))
  IHash       (-hash    [_]    (hash object))
  IWithMeta
  (-with-meta [_this m]
    (RuntimeAtom.
     runtime
     (assoc object :meta m)))
  IPrintWithWriter
  (-pr-writer [this writer _opts]
    (runtime-print this writer _opts)))

(defn runtime? [value]
  (satisfies? Runtime value))

(defn ->runtime [call object]
  (if (and
       (not= (:tag object) :var)
       (contains? (:protocols object) :IDeref))
    (->RuntimeAtom call object)
    (->RuntimeObject call object)))

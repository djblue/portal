(ns portal.client
  #?(:clj  (:require [portal.client.jvm :as p])
     :cljr (:require [portal.client.clr :as p])
     :cljs (:require [portal.client.node :as p])
     :lpy  (:require [portal.client.py :as p])
     :jank (:require [portal.client.jank :as p]))
  #?(:cljr (:import (System Environment))
     :lpy  (:import [os :as os])))

(def ^:private port
  #?(:clj  (System/getenv "PORTAL_PORT")
     :cljr (Environment/GetEnvironmentVariable "PORTAL_PORT")
     :cljs (.-PORTAL_PORT js/process.env)
     :lpy  (.get os/environ "PORTAL_PORT")
     :jank (cpp/std.getenv "PORTAL_PORT")))

(defn enabled? [] (some? port))

(defn submit [value]
  (p/submit {:port port :encoding :cson} value))
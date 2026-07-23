(ns ^:no-doc portal.runtime.os
  #?(:cljr (:import (System Environment))
     :lpy  (:import (os :as os))))

(defn env [variable]
  #?(:clj  (System/getenv variable)
     :cljs (aget js/process.env variable)
     :cljr (Environment/GetEnvironmentVariable variable)
     :lpy  (.get os/environ variable)))

(defn wsl? [] (some? (env "WSL_DISTRO_NAME")))
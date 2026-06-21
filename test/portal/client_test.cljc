(ns portal.client-test
  #?(:clj
     (:require [clojure.test :refer [deftest is]]
               [portal.api :as p]
               [portal.client.jvm :as c]
               [portal.runtime :as rt])
     :cljr
     (:require [clojure.test :refer [deftest is]]
               [portal.api :as p]
               [portal.client.clr :as c]
               [portal.runtime :as rt])
     :cljs
     (:require [clojure.test :refer [async deftest is]]
               [portal.api :as p]
               [portal.async :as a]
               [portal.client.node :as c]
               [portal.runtime :as rt])
     :lpy
     (:require [clojure.test :refer [deftest is]]
               [portal.api :as p]
               [portal.client.py :as c]
               [portal.runtime :as rt])
     :jank
     (:require [clojure.test :refer [deftest is]]
               [portal.api :as p]
               [portal.client.jank :as c]
               [portal.runtime :as rt])))

(def ^:private bad-seq (map (fn [_] (throw (ex-info "Error" {}))) (range 10)))

(defn- client-test* []
  (#?(:cljs a/let :default let)
   [server (p/start
            #?(:jank {:port 9999} ;; httplib.h has issues during test time with dynamic ports
               :default {}))
    opts (select-keys server [:port :host])
    tap-list @#'rt/tap-list]
    (swap! tap-list empty)
    (c/submit opts ::value)
    (c/submit opts bad-seq)
    (is (= "Error"
           (or (:cause (first @tap-list))
               (:error (first @tap-list)))))
    (is (= ::value (second @tap-list)))))

(deftest client-test
  #?(:cljs    (async done (.then (client-test*) done))
     :default (client-test*)))
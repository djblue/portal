(ns portal.runtime.jvm.ssr-test
  (:refer-clojure :exclude [random-uuid])
  (:require [clojure.test :refer [deftest is]]
            [examples.data :refer [data]]
            [portal.runtime :as rt]
            [portal.runtime.jvm.ssr :as ssr]
            [portal.runtime.polyfill :refer [random-uuid]]))

(deftest render-app-test
  (let [session-id (random-uuid)
        value (atom data)
        channel (fn send! [_])
        session (#'ssr/open-session
                 {:session
                  {:channel channel
                   :session-id session-id
                   :options {:value value}}})]
    (try
      (#'ssr/on-open session)
      (is (contains? @rt/connections session-id))
      (is (contains? @@#'ssr/render-loops session-id))
      (finally
        (#'ssr/on-close session)))
    (is (not (contains? @rt/connections session-id)))
    (is (not (contains? @@#'ssr/render-loops session-id)))))
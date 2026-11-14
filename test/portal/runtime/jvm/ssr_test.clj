(ns portal.runtime.jvm.ssr-test
  (:refer-clojure :exclude [random-uuid])
  (:require [clojure.test :refer [deftest is]]
            [examples.data :refer [data]]
            [portal.runtime :as rt]
            [portal.runtime.jvm.ssr :as ssr]
            [portal.runtime.polyfill :refer [random-uuid]]))

(deftest render-app-test
  (let [session-id (random-uuid)
        connection-id (random-uuid)
        value   (atom data)
        channel (fn send! [_])
        opts    {:session-id session-id
                 :options {:value value}}]
    (try
      (#'ssr/on-open session-id opts)
      (#'ssr/on-connect session-id connection-id channel)
      (is (contains? @rt/connections session-id))
      (is (contains? @@#'ssr/render-loops session-id))
      (is (contains? @@#'ssr/sessions session-id))
      (finally
        (#'ssr/on-disconnect session-id connection-id)
        (#'ssr/on-close session-id)))
    (is (not (contains? @rt/connections session-id)))
    (is (not (contains? @@#'ssr/render-loops session-id)))
    (is (not (contains? @@#'ssr/sessions session-id)))))
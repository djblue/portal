(ns portal.runtime.rpc-test
  (:refer-clojure :exclude [random-uuid])
  (:require [clojure.test :refer [deftest is]]
            [portal.runtime :as rt]
            [portal.runtime.cson :as cson]
            [portal.runtime.cson.core :as core]
            [portal.runtime.polyfill :refer [random-uuid]]
            [portal.runtime.rpc :as rpc]))

(def ^:dynamic *rpc-cson* nil)

(defn- client-rpc [session f & args]
  (binding [*rpc-cson* (atom nil)]
    (rpc/on-receive
     session
     (cson/write
      {:portal.rpc/id (random-uuid)
       :op :portal.rpc/invoke
       :f f
       :args args}))
    (:return
     (cson/read
      @*rpc-cson*
      {:default-handler core/tagged-value}))))

(deftest rpc-init
  (let [session-id (random-uuid)
        value (atom [::value])
        options {:value value}
        _ (swap! rt/sessions update-in [session-id :options] merge options)
        session (rt/open-session {:session-id session-id})]
    (try
      (rpc/on-open session (fn [cson] (reset! *rpc-cson* cson)))
      (let [client-options (client-rpc session `rt/get-options)]
        (is (contains? @(:watch-registry session) value))
        (is (= ::rt/pong (client-rpc session `rt/ping)))
        (let [value-ref (core/tagged-value
                         "ref"
                         (get-in (:value client-options) [:rep :id]))
              functions (client-rpc session `rt/get-functions value-ref)]
          (is (contains? functions `clojure.core/deref))
          (is (= @value
                 (client-rpc
                  session
                  `clojure.core/deref
                  value-ref))
              "The server can deref runtime atom by #ref")

          (client-rpc session `rt/clear-values)
          (is (empty? @value))

          (is (contains? functions `portal.api/toggle-watch))
          (client-rpc session `portal.api/toggle-watch value-ref)
          (is (not (contains? @(:watch-registry session) value)))))
      (finally
        (rt/close-session session-id)
        (swap! rt/sessions dissoc session-id)))))
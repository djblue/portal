(ns portal.ui.state-test
  (:require [clojure.test :refer [deftest is]]
            [portal.ui.state :as state]))

(deftest expanded-test
  (let [state nil
        context {:value :hi :stable-path [] :depth 1}]
    (is (nil? (state/expanded? state context))))
  (let [context  {:value :hi :stable-path [] :depth 1}
        location (state/get-location context)
        state    {:expanded? {location 0}}]
    (is (false? (state/expanded? state context))))
  (let [context  {:value :hi :stable-path [] :depth 1}
        location (state/get-location context)
        state    {:expanded? {location 1}}]
    (is (true? (state/expanded? state context))))
  (let [parent   {:value [:hi] :stable-path [] :depth 1}
        context  {:value :hi :stable-path [0] :depth 2 :parent parent}
        location (state/get-location parent)
        state-a  {:expanded? {location 1}}
        state-b  {:expanded? {location 2}}]
    (is (nil?  (state/expanded? state-a context)))
    (is (true? (state/expanded? state-b context)))))

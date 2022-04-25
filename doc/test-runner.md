# Test Runner

With the addition of `:portal.viewer/test-report`, Portal can now render your
`clojure.test` output!

However, there is no automatic mechanism for delivering test report data
directly to Portal, that integration is up to you.

## Example Integration

To get started, let's assume you have the following tests:

```clojure
(ns user
  (:require [clojure.test :refer [deftest is] :as t]))

(deftest hello-world
  (is (= 0 0))
  (is (= (name :hello) (name :world))))
```

There are many ways to run clojure tests, but an easy way is via
[clojure.test/run-tests](https://clojuredocs.org/clojure.test/run-tests), so
let's go with that.

```clojure
(defn run-test [ns]
  (let [report (atom [])]
    (tap> report)
    (with-redefs [t/report #(swap! report conj %)]
      (t/run-tests ns))))

(run-test 'user)
```

That's pretty much it. The key here is that you simply need to intercept values
passed to [`clojure.test/report`](https://clojuredocs.org/clojure.test/report)
and send them directly to Portal.

With the code above, you should get something like:



Although this example is trivial, the main advantage to getting test result out
as data is particularly handing we dealing with large values in a test
assertion.

### Tip

You can select test output values and diff them via the
[`lambdaisland.deep-diff2/diff` command](./ui/commands.md)

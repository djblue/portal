# [Remote API](./examples/remote/)

In addition to running portal in process, values can be sent over the wire to a
remote instance.

In the process hosting the remote api, do:

``` clojure
(require '[portal.api :as p])
(p/open {:port 5678})
```

In the client process, do:

``` clojure
(require '[portal.client.jvm :as p])
;; (require '[portal.client.node :as p])
;; (require '[portal.client.web :as p])

(def submit (partial p/submit {:port 5678})) ;; :encoding :edn is the default
;; (def submit (partial p/submit {:port 5678 :encoding :json}))
;; (def submit (partial p/submit {:port 5678 :encoding :transit}))

(add-tap #'submit)
```

**NOTE:** `tap>`'d values must be serializable as edn, transit or json.

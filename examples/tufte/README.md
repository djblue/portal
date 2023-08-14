# Tufte Setup Guide

If you are a [ptaoussanis/tufte](https://github.com/ptaoussanis/tufte) user,
this guide will help you get profiling data into Portal.

![Screenshot 2023-07-25 at 8 13 10 PM](https://github.com/djblue/portal/assets/1986211/d9532ca7-af28-4e33-a592-8704d589c371)

The main advantages of using Portal are that your profiling data is always
available as data and the `:loc` data can be used to jump directly to the source
location using the `goto-definition` [command][commands].

### Setup

To get started, you need the following namespaces:

``` clojure
(ns user
  (:require [portal.api :as p]
            [taoensso.tufte :as tufte :refer (p profiled profile)]))
```

Next, you need to map tufte pstats data to something that can be used by the
Portal table viewer. Below is one such mapping:

```clojure
(def columns
  (-> [:min :p25 :p50 :p75 :p90 :p95 :p99 :max :mean :mad :sum]
      (zipmap (repeat :portal.viewer/duration-ns))
      (assoc :loc :portal.viewer/source-location)))

(defn format-data [stats]
  (-> stats
      (update-in [:loc :ns] symbol)
      (vary-meta update :portal.viewer/for merge columns)))

(defn format-pstats [pstats]
  (-> @pstats
      (:stats)
      (update-vals format-data)
      (with-meta
        {:portal.viewer/default :portal.viewer/table
         :portal.viewer/table
         {:columns [:n :min #_:p25 #_:p50 #_:p75 #_:p90 #_:p95 #_:p99 :max :mean #_:mad :sum :loc]}})))
```

With the above mapping, you can plug into tufte via its handler mechanism:

```clojure
(defn add-tap-handler!
  "Adds a simple handler that logs `profile` stats output with `tap>`."
  [{:keys [ns-pattern handler-id]
    :or   {ns-pattern "*"
           handler-id :basic-tap}}]
  (tufte/add-handler!
   handler-id ns-pattern
   (fn [{:keys [?id ?data pstats]}]
     (tap> (vary-meta
            (format-pstats pstats)
            merge
            (cond-> {}
              ?id   (assoc :id ?id)
              ?data (assoc :data ?data)))))))
```

### 10-second example

Borrowing from the [10 second example in the tufte docs][tufte-example], we have
the equivalent:

```clojure
;; Open the Portal UI to see the output:
(require '[portal.api :refer (open submit)])
(add-tap submit)
(open)

(require '[taoensso.tufte :as tufte :refer (defnp p profiled profile)])

;; We'll request to send `profile` stats to `tap>`:
(add-tap-handler! {})

;;; Let's define a couple dummy fns to simulate doing some expensive work
(defn get-x [] (Thread/sleep 500)             "x val")
(defn get-y [] (Thread/sleep (rand-int 1000)) "y val")

;; How do these fns perform? Let's check:

(profile ; Profile any `p` forms called during body execution
  {} ; Profiling options; we'll use the defaults for now
  (dotimes [_ 5]
    (p :get-x (get-x))
    (p :get-y (get-y))))
```

> **Note**
> goto-definition will only work if `taoensso.tufte/p` calls are eval'd
> via a repl that properly provide file/line/column info. This includes nrepl
> or using load-file to load your code.

[tufte-example]: https://github.com/ptaoussanis/tufte#10-second-example
[commands]: ../../doc/ui/commands.md

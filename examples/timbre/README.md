# Timbre Setup Guide

If you are a [com.taoensso/timbre](https://github.com/ptaoussanis/timbre) user,
this guide will help you setup a portal instance dedicated to your timbre logs.

<img width="1209" alt="Timbre Log Viewer" src="https://user-images.githubusercontent.com/1986211/164835347-8cc69359-83cd-42e4-9e19-938155767118.png">

## Setup

To get started, you need the following namespaces:

``` clojure
(ns user
  (:require [clojure.datafy :as d]
            [clojure.instant :as i]
            [portal.api :as p]
            [taoensso.timbre :as log]))
```

Next, you need to map timbre log data to something that can be used by the
portal log viewer. Your timbre logs might have more info, so feel free to
customize the mapping to include more data.

``` clojure
(defn log->portal [{:keys [level ?err msg_ timestamp_ ?ns-str ?file context ?line]}]
  (merge
   (when ?err
     {:error (d/datafy ?err)})
   (when-let [ts (force timestamp_)]
     {:time (i/read-instant-date ts)})
   {:level   level
    :ns      (symbol (or ?ns-str ?file "?"))
    :line    (or ?line 1)
    :column  1
    :result  (force msg_)
    :runtime :clj}
   context))
```

Next, you need to setup an atom to store the rolling set of logs and wire it
into timbre:

``` clojure
(defonce logs (atom '()))

(defn log
  "Accumulate a rolling log of 100 entries."
  [log]
  (swap! logs
         (fn [logs]
           (take 100 (conj logs (log->portal log))))))

(defn setup []
  (reset! logs '())
  (log/merge-config!
   {:appenders
    {:memory {:enabled? true :fn log}}}))
```

Finally, we can open a dedicated instance of portal which will receive the logs:

```clojure
(defn open []
  (p/open {:window-title "Logs Viewer" :value logs}))
```

Putting everything together, you can now do the following at the REPL:

``` clojure
(setup)
(open)

(log/info  "my info log")
(log/error (ex-info "my exception" {:hello :world}) "my error log")
```

## ClojureScript Logs

Also, if you are using timbre for cljs logs, you can add the following context
to differentiate clj from cljs logs:

``` clojure
(log/with-context+
  {:runtime :cljs}
  (log/info "my cljs log"))
```

<img width="1209" alt="Timbre CLJS Log Viewer" src="https://user-images.githubusercontent.com/1986211/164836119-11695d5f-672f-444c-a908-c0f157056f66.png">

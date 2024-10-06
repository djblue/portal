# [Clerk](https://github.com/nextjournal/clerk)

If you would like to leverage Portal in the context of a clerk notebook, you can
setup a viewer such as the following:

> [!WARNING] 
> Every viewer is a new iframe so having too many on the page will
> cause performance issues.

```clojure
(ns portal.clerk
  (:require [nextjournal.clerk :as clerk]
            [portal.api :as p]))

(def app-viewer
  {:name :portal/app
   :transform-fn
   (fn [value]
     (p/url
      (p/open {:launcher false
               :value    (:nextjournal/value value)
               :theme    :portal.colors/nord-light})))
   :render-fn '#(v/html [:iframe
                         {:src %
                          :style {:width "100%"
                                  :height "50vh"
                                  :border-left "1px solid #d8dee9"
                                  :border-right "1px solid #d8dee9"
                                  :border-bottom "1px solid #d8dee9"
                                  :border-radius 2}}])})

(defn open
  "Open portal with the value of `x` in current notebook."
  ([x] (open {} x))
  ([viewer-opts x]
   (clerk/with-viewer app-viewer viewer-opts x)))
```

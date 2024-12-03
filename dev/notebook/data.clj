(ns notebook.data
  (:require
   [examples.data :as d]
   [portal.colors :as c]
   [portal.viewer :as v]))

(::d/hacker-news d/data)

(-> d/basic-data)

(-> d/platform-data)

(:portal.colors/nord c/themes)

(-> d/diff-data)

(-> d/prepl-data)

(-> d/log-data)

(v/table d/log-data)

(-> d/hiccup)

(v/tree d/hiccup)

(v/hiccup
  [:portal.viewer/markdown (::d/markdown d/string-data)])

(-> d/exception-data)

(-> d/test-report)

(-> d/line-chart)

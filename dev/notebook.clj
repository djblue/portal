(ns notebook
  (:require [examples.data :as d]
            [portal.colors :as c]))

(defn viewer
  "Set default portal viewer for a given value."
  [v default]
  (with-meta v {:portal.viewer/default default}))

(-> d/basic-data)

(-> d/platform-data)

(:portal.colors/nord c/themes)

(-> d/diff-data)

(-> d/prepl-data)

(-> d/log-data)

(viewer d/log-data :portal.viewer/table)

(-> d/hiccup)

(viewer d/hiccup :portal.viewer/tree)

(viewer
 [:portal.viewer/markdown (::d/markdown d/string-data)]
 :portal.viewer/hiccup)

(-> d/exception-data)

(-> d/test-report)

(-> d/line-chart)
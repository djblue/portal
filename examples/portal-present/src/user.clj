(ns user
  (:require [clojure.java.io :as io]
            [portal.api :as p]
            [portal.viewer :as-alias v]))

(defn with-viewer [viewer]
  (with-meta
    [:<>
     [:h1 [::v/inspector viewer]]
     [:div
      {:style
       {:padding 40}}
      [viewer [:h1 "hello, world"]]]]
    {::v/default ::v/hiccup}))

(def slides (atom nil))

(reset!
 slides
 (with-meta
   [(with-viewer ::v/inspector)
    (with-viewer ::v/hiccup)
    (with-viewer ::v/tree)
    (with-viewer ::v/pprint)
    (with-viewer ::v/pr-str)]
   {::v/default :portal-present.viewer/slides}))

(declare presentation)

(defn on-load []
  (p/eval-str presentation
              (slurp "examples/portal-present/src/portal_present/viewer.cljs")
              #_(slurp (io/resource "portal_present/viewer.cljs"))))

(def presentation
  (p/open {:mode :dev
           :value slides
           :on-load on-load
           :launcher :vs-code
           :window-title "Portal Present"}))

(comment
  ;; for debugging
  (p/open {:mode :dev :launcher :vs-code})
  (swap! slides empty)
  (add-tap p/submit)
  ;; start nrepl subrepl
  (p/repl presentation)
  (-> :cljs/quit))

(ns examples.default-visualizer
  (:require
   [clojure.core.protocols :refer [nav]]
   [clojure.string :as str]))

(defn nav-dep-anno-tree [coll _ v]
  (let [{:keys [deps-map]} (meta coll)]
    (with-meta
      [:div "Depends on "
       (str/join ", " (map str (get deps-map v)))]
      {:portal.viewer/default :portal.viewer/hiccup})))

(comment
  (tap>
   (with-meta
     {:a 1
      :b 2
      :c 3}
     {`nav      #'nav-dep-anno-tree
      :deps-map {:c #{:b :a}}})))
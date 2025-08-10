(ns portal.user
  (:require [portal.ui.api :as p]))

(defn viewer [_] ::hi)

(p/register-viewer!
 {:name ::viewer
  :predicate (constantly true)
  :component  #'viewer})
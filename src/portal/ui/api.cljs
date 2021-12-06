(ns portal.ui.api
  (:require [reagent.core :as r]))

(defonce viewers (r/atom (list)))

(defn register-viewer! [viewer-spec]
  (swap! viewers conj viewer-spec))

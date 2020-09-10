(ns portal.ui.state
  (:require [reagent.core :as r]))

(defonce state     (r/atom nil))
(defonce tap-state (r/atom nil))

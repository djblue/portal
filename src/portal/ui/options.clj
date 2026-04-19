(ns ^:no-doc portal.ui.options
  (:require [portal.ui.react :as react]
            [portal.ui.state :as state]))

(defonce ^:private options-context (react/create-context nil))

(defn ^:no-doc with-options* [options & children]
  (apply react/provider options-context options children))

(defn with-options [& children]
  (let [[options set-options!] (react/use-state ::loading)]
    (react/use-effect
     :once
     (set-options! (state/invoke `portal.runtime/get-options)))
    (into [with-options* options] children)))

(defn use-options [] (react/use-context options-context))
(ns portal.ui.api
  (:require [portal.viewer :as v]
            #?(:cljs [reagent.core :as r])))

(defonce viewers
  #?(:clj  (atom (v/table [] {:columns [:name :doc]}))
     :cljs (r/atom (v/table [] {:columns [:name :doc]}))))

(defn register-viewer! [viewer-spec]
  (swap! viewers
         (fn [viewers]
           (assoc
            viewers
            (or
             (first
              (keep-indexed
               (fn [index {:keys [name]}]
                 (when (= (:name viewer-spec) name)
                   index))
               viewers))
             (count viewers))
            viewer-spec))))

#?(:cljs (def ^:no-doc portal-api "Portal API for JS interop." #js {}))

#?(:cljs (set! (.-portal_api js/window) portal-api))
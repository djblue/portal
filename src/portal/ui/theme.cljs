(ns portal.ui.theme
  (:require ["react" :as react]
            [portal.colors :as c]))

(defn- get-theme [theme-name]
  (merge
   {:font/family "monospace"
    :font-size "12pt"
    :limits/string-length 100
    :limits/max-depth 1
    :spacing/padding 8
    :border-radius 2}
   (get c/themes theme-name)))

(defonce ^:private theme
  (react/createContext (get-theme ::c/nord)))

(defn with-theme [theme-name & children]
  (into [:r> (.-Provider theme)
         #js {:value (get-theme theme-name)}]
        children))

(defn use-theme [] (react/useContext theme))

(defonce ^:private order
  (cycle [::c/exception ::c/keyword ::c/string ::c/tag ::c/number ::c/uri]))

(defonce ^:private rainbow
  (react/createContext order))

(defn cycle-rainbow [& children]
  (let [order (react/useContext rainbow)]
    (into [:r> (.-Provider rainbow)
           #js {:value (rest order)}]
          children)))

(defn use-rainbow []
  (let [theme (use-theme)
        order (react/useContext rainbow)]
    (get theme (first order))))

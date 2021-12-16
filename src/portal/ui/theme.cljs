(ns portal.ui.theme
  (:require ["react" :as react]
            [portal.colors :as c]
            [portal.ui.options :as opts]))

(defn ^:no-doc is-vs-code? []
  (-> js/document
      .-documentElement
      js/getComputedStyle
      (.getPropertyValue "--vscode-font-size")
      (not= "")))

(defn- get-theme [theme-name]
  (let [opts (opts/use-options)]
    (merge
     {:font-family "Monaco, monospace"
      :font-size "12pt"
      :string-length 100
      :max-depth 1
      :padding 8
      :border-radius 2}
     (or (get c/themes theme-name)
         (get (:themes opts) theme-name)))))

(defn- default-theme []
  (if (is-vs-code?) ::c/vs-code-embedded ::c/nord))

(defonce ^:private theme (react/createContext nil))

(defn with-theme [theme-name & children]
  (into [:r> (.-Provider theme)
         #js {:value (get-theme (or theme-name (default-theme)))}]
        children))

(defn use-theme [] (react/useContext theme))

(defonce ^:no-doc order
  (cycle [::c/diff-remove ::c/diff-add ::c/keyword ::c/tag ::c/number ::c/uri]))

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

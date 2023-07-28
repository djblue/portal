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
     {:font-family   "monospace"
      :font-size     "12pt"
      :string-length 100
      :max-depth     2
      :padding       6
      :border-radius 2}
     (or (get c/themes theme-name)
         (get (:themes opts) theme-name)))))

(defn- use-theme-detector []
  (let [media-query (.matchMedia js/window "(prefers-color-scheme: dark)")
        [dark-theme set-dark-theme!] (react/useState (.-matches media-query))]
    (react/useEffect
     (fn []
       (let [listener (fn [e] (set-dark-theme! (.-matches e)))]
         (.addListener media-query listener)
         (fn []
           (.removeListener media-query listener))))
     #js [])
    dark-theme))

(defn- default-theme [dark-theme]
  (cond
    (is-vs-code?) ::c/vs-code-embedded
    dark-theme    ::c/nord
    :else         ::c/nord-light))

(defonce ^:private theme-context (react/createContext nil))

(defn use-theme [] (react/useContext theme-context))

(defn with-theme+ [theme & children]
  (let [theme (merge (use-theme) theme)]
    (into [:r> (.-Provider theme-context) #js {:value theme}] children)))

(defn with-theme [theme-name & children]
  (let [dark-theme (use-theme-detector)
        theme      (get-theme (or theme-name (default-theme dark-theme)))
        background (::c/background theme)]
    (react/useEffect
     #(set! (.. js/document -body -style -backgroundColor)
            background)
     #js [background])
    (into [:r> (.-Provider theme-context) #js {:value theme}] children)))

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

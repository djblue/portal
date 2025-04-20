(ns portal.ui.theme
  (:require [clojure.string :as str]
            [portal.colors :as c]
            [portal.ui.options :as opts]
            [portal.ui.react :as react]))

(defn ^:no-doc is-vs-code? []
  (-> js/document
      .-documentElement
      js/getComputedStyle
      (.getPropertyValue "--vscode-font-size")
      (not= "")))

(defn- get-style []
  (some-> js/document
          (.getElementsByTagName "html")
          (aget 0)
          (.getAttribute "style")
          not-empty))

(defn ^:no-doc get-vs-code-css-vars []
  (when-let [style (get-style)]
    (persistent!
     (reduce
      (fn [vars rule]
        (if-let [[attr value] (str/split rule #"\s*:\s*")]
          (assoc! vars (str "var(" attr ")") value)
          vars))
      (transient {})
      (str/split style #"\s*;\s*")))))

(defn- get-theme [theme-name]
  (let [opts (opts/use-options)
        vars (get-vs-code-css-vars)]
    (merge
     {:font-family   "monospace"
      :font-size     "12pt"
      :string-length 100
      :max-depth     2
      :padding       6
      :border-radius 2}
     (update-vals
      (or (get c/themes theme-name)
          (get (:themes opts) theme-name))
      #(get vars % %)))))

(defn- use-theme-detector []
  (let [media-query (.matchMedia js/window "(prefers-color-scheme: dark)")
        [dark-theme set-dark-theme!] (react/use-state (.-matches media-query))]
    (react/use-effect
     :once
     (let [listener (fn [e] (set-dark-theme! (.-matches e)))]
       (.addListener media-query listener)
       (fn []
         (.removeListener media-query listener))))
    dark-theme))

(defn- use-vscode-theme-detector []
  (let [[change-id set-change-id!] (react/use-state 0)]
    (when (is-vs-code?)
      (react/use-effect
       :once
       (let [observer (js/MutationObserver. #(set-change-id! inc))]
         (.observe observer
                   js/document.documentElement
                   #js {:attributes true
                        :attributeFilter #js ["style"]})
         #(.disconnect observer))))
    change-id))

(defn- default-theme [dark-theme]
  (cond
    (is-vs-code?) ::c/vs-code-embedded
    dark-theme    ::c/nord
    :else         ::c/nord-light))

(defonce ^:private theme-context (react/create-context nil))

(defn use-theme [] (react/use-context theme-context))

(defn with-theme+ [theme & children]
  (let [theme (merge (use-theme) theme)]
    (into [:r> (.-Provider theme-context) #js {:value theme}] children)))

(defn ^:no-doc with-background []
  (let [background (::c/background (use-theme))]
    (react/use-effect
     #js [background]
     (set! (.. js/document -body -style -backgroundColor) background))
    nil))

(defn with-theme [theme-name & children]
  (let [dark-theme (use-theme-detector)
        theme-key  (use-vscode-theme-detector)
        theme      (get-theme (or theme-name (default-theme dark-theme)))]
    (into [:r> (.-Provider theme-context) #js {:key theme-key :value theme}] children)))

(defonce ^:no-doc order
  (cycle [::c/diff-remove ::c/diff-add ::c/keyword ::c/tag ::c/number ::c/uri]))

(defonce ^:private rainbow
  (react/create-context order))

(defn cycle-rainbow [& children]
  (let [order (react/use-context rainbow)]
    (into [:r> (.-Provider rainbow)
           #js {:value (rest order)}]
          children)))

(defn use-rainbow []
  (let [theme (use-theme)
        order (react/use-context rainbow)]
    (get theme (first order))))

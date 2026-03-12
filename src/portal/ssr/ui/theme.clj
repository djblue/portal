(ns portal.ssr.ui.theme
  (:require [portal.colors :as c]
            [portal.ssr.ui.react :as react]))

(defn- get-theme [theme-name]
  (when-let [theme (get c/themes theme-name)]
    (merge
     {:font-family   "monospace"
      :font-size     "12pt"
      :string-length 100
      :max-depth     2
      :padding       6
      :border-radius 2}
     theme)))

(defonce ^:private theme-context (react/create-context nil))

(defn use-theme [] (react/use-context theme-context))

(defn with-theme [theme-name & children]
  (let [;dark-theme (use-theme-detector)
        ;theme-key  (use-vscode-theme-detector)
        theme      (get-theme theme-name)
        #_(or (get-theme theme-name)
              (get-theme (default-theme dark-theme)))]
    (apply react/provider theme-context theme children)))

(defn with-theme+ [theme & children]
  (let [theme (merge (use-theme) theme)]
    (apply react/provider theme-context theme children)))

(defonce ^:no-doc order
  (cycle [::c/diff-remove ::c/diff-add ::c/keyword ::c/tag ::c/number ::c/uri]))

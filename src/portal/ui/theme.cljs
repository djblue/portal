(ns portal.ui.theme
  (:require ["react" :as react]
            [portal.colors :as c]))

(defn- is-vs-code? []
  (-> js/document
      .-documentElement
      js/getComputedStyle
      (.getPropertyValue "--vscode-font-size")
      (not= "")))

(defn- vs-code-vars []
  (when (is-vs-code?)
    {:font-size "var(--vscode-font-size)"
     :font-family "var(--vscode-editor-font-family)"
     :border-radius 0
     :padding 8
     ::c/background "var(--vscode-menu-background)"
     ::c/background2 "var(--vscode-editor-background)"
     ::c/border "var(--vscode-editorWidget-border)"
     ::c/text "var(--vscode-foreground)"
     ::c/string "var(--vscode-debugTokenExpression-string)"
     ::c/number "var(--vscode-debugTokenExpression-number)"
     ::c/boolean "var(--vscode-debugTokenExpression-boolean)"
     ::c/keyword "var(--vscode-debugTokenExpression-value)"
     ::c/symbol "var(--vscode-debugTokenExpression-name)"
     ::c/exception "var(--vscode-errorForeground)"
     ::c/diff-add "var(--vscode-terminal-ansiGreen)"
     ::c/diff-remove "var(--vscode-debugTokenExpression-name)"}))

(defn- get-theme [theme-name]
  (merge
   {:font-family "Monaco, monospace"
    :font-size "12pt"
    :string-length 100
    :max-depth 1
    :padding 8
    :border-radius 2}
   (get c/themes theme-name)
   (vs-code-vars)))

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

(ns portal.user
  (:require
   [portal.colors :as c]
   [portal.ui.api :as p]))

(defn viewer [_] (str ::hi))

(p/register-viewer!
 {:name ::viewer
  :predicate (constantly true)
  :component  #'viewer})

(c/register!
 ::surprising-blueberry
 {::c/reference   "https://github.com/BeardedBear/bearded-theme"
  ::c/string      "#a9db76"
  ::c/keyword     "#c93e71"
  ::c/exception   "#c13736"
  ::c/text        "#bacbe4"
  ::c/background2 "#0d1521"
  ::c/boolean     "#c93e71"
  ::c/tag         "#eacd61"
  ::c/uri         "#b85c40"
  ::c/namespace   "#cc9b52"
  ::c/diff-add    "#a9dc76"
  ::c/package     "#01b3bc"
  ::c/border      "rgba(103, 47, 73, 0.35)"
  ::c/symbol      "#01b3bc"
  ::c/diff-remove "#c13736"
  ::c/number      "#b5cea8"
  ::c/background  "#101a29"
  :font-size    "var(--vscode-editor-font-size, 12pt)"
  :font-family  "var(--vscode-editor-font-family, Monaco)"
  :border-radius 0})
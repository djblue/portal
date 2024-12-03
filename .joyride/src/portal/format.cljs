(ns portal.format
  (:require
   ["path" :as path]
   ["vscode" :as vscode]))

(defn- get-workspace-folder []
  (let [^js uri (-> vscode/workspace .-workspaceFolders (aget 0) .-uri)
        fs-path (.-fsPath uri)]
    (if-not (undefined? fs-path) fs-path (.-path uri))))

(defn- get-standard-npm-path []
  (path/join (get-workspace-folder)
    "node_modules"
    "@chrisoakman/standard-clojure-style"
    "dist/standard-clojure-style.js"))

(def ^:private standard (js/require (get-standard-npm-path)))

;; (js/require "@chrisoakman/standard-clojure-style")

;; ("./package.json")

;; (defn- require-string [src file-name]
;;   (let [Module (js/require "module")
;;         ^js m (Module. file-name (some-> js/module .-parent))]
;;     (set! (.-filename m) file-name)
;;     (._compile m src file-name)
;;     (.-exports m)))

;; (defn require-url [url]
;;   (-> (js/fetch url)
;;       (.then #(.text %))
;;       (.then #(require-string % "standard-clojure-style.js"))))

;; (defonce lib (atom nil))

;; (when-not @lib
;;   (-> (require-url "https://raw.githubusercontent.com/oakmac/standard-clojure-style-js/refs/heads/master/lib/standard-clojure-style.js")
;;       (.then #(reset! lib %))))

(defn formatter [model _options _token]
  (let [code (.-out (.format standard (.getText model)))]
    #js [(.replace vscode/TextEdit
           (vscode.Range.
             (.. (.lineAt model 0) -range -start)
             (.. (.lineAt model (dec (.-lineCount model))) -range -end))
           code)]))

(comment
  (.registerDocumentFormattingEditProvider
    vscode/languages "clojure"
    #js {:displayName "Clojure Standard Formatter"
         :provideDocumentFormattingEdits (fn [& args] (apply formatter args))})

  (require '[portal.api :as p])
  (p/open {:launcher :vs-code})
  (add-tap #'p/submit)

  ,)
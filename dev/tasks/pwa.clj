(ns tasks.pwa
  (:require
   [babashka.fs :as fs]
   [clojure.java.browse :refer [browse-url]]
   [clojure.java.io :as io]
   [hiccup.core :refer [html]]
   [portal.colors :as c]
   [portal.runtime.cson :as cson]
   [portal.runtime.json :as json]
   [tasks.build :refer [install]]
   [tasks.docs :as docs]
   [tasks.tools :refer [shadow]]))

(defn- manifest-json [settings]
  (json/write
    {:short_name "Portal"
     :name (:name settings)
     :description "A clojure tool to navigate through your data."
     :icons
     [{:type "image/svg+xml"
       :sizes "512x512"
       :src "icon.svg"}]
     :handle_links "preferred"
     :scope (:host settings)
     :start_url (:host settings)
     :display "standalone"
     :display_override ["minimal-ui"]}))

(defn- index-html [settings]
  (str
    "<!DOCTYPE html>"
    (html
      [:html
       {:lang "en"}
       [:head
        [:title "portal"]
        [:meta {:charset "UTF-8"}]
        [:meta {:name "viewport"
                :content "width=device-width, initial-scale=1"}]
        [:link {:rel :manifest :href "manifest.json"}]
        [:meta {:name "theme-color" :content (::c/background2 settings)}]]
       [:body
        {:style
         (identity ;; because hiccup is a macro
           {:margin 0
            :overflow "hidden"
            :min-height "100vh"
            :background (::c/background settings)})}
        [:div {:id "root"}]
        [:script {:src "main.js"}]]])))

(defmethod print-method (Class/forName "[B") [v ^java.io.Writer w]
  (.write w "#portal/bin \"")
  (.write w (cson/base64-encode v))
  (.write w "\""))

(comment (remove-method print-method (Class/forName "[B")))

(defn- ->edn [v] (binding [*print-meta* true] (pr-str v)))

(defn- get-files [settings]
  (let [settings (merge settings (::c/nord c/themes))]
    {"index.html"     (index-html settings)
     "manifest.json"  (manifest-json settings)
     "icon.svg"       (slurp (io/resource "icon.svg"))
     "sw.js"          (slurp (io/resource "sw.js"))
     "docs.edn"       (->edn (docs/gen-docs))}))

(def envs
  {:dev  {:name "portal-dev"
          :dir "target/pwa/"
          :host "http://localhost:4400"}
   :prod {:name "portal"
          :dir "target/pwa-release/"
          :host "https://djblue.github.io/portal/"}})

(defn generate-files [env]
  (let [{:keys [dir] :as settings} (get envs (keyword env))]
    (doseq [[file content] (get-files settings)]
      (spit (str dir file) content))))

(defn pwa
  "Build portal PWA. (djblue.github.io/portal)"
  []
  (install)
  (fs/create-dirs "target/pwa-release/")
  (generate-files :prod)
  (shadow :release :pwa))

(defn -main [] (pwa))

(comment
  (generate-files :dev)
  (generate-files :prod)
  (browse-url "http://localhost:4400")

  (require '[portal.api :as p])
  (require '[portal.runtime.browser :as browser])
  (with-redefs [browser/pwa (:dev envs)]
    (def portal (p/open {:mode :dev}))))

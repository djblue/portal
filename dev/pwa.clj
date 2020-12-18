(ns pwa
  (:require [cheshire.core :as json]
            [clojure.java.browse :refer [browse-url]]
            [clojure.java.io :as io]
            [hiccup.core :refer [html]]
            [portal.colors :as c]))

(defn manifest-json [settings]
  (json/generate-string
   {:short_name "Portal"
    :name "portal"
    :description "A clojure tool to navigate through your data."
    :icons
    [{:type "image/svg+xml"
      :sizes "512x512"
      :src "icon.svg"}]
    :scope (::host settings)
    :start_url (::host settings)
    :display "standalone"
    :display_override ["tabbed" "minimal-ui"]}
   {:pretty true}))

(defn index-html [settings]
  (html
   [:html
    [:head
     [:meta {:charset "UTF-8"}]
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
     [:script {:src "main.js"}]]]))

(def hosts
  {"target/pwa/"         "http://localhost:4400"
   "target/pwa-release/" "https://djblue.github.io/portal/"})

(defn get-files [dir]
  (let [settings (merge {::host (get hosts dir)}
                        (::c/nord c/themes))]
    {"index.html"     (index-html settings)
     "manifest.json"  (manifest-json settings)
     "icon.svg"       (slurp (io/resource "icon.svg"))
     "sw.js"          (slurp (io/resource "sw.js"))}))

(defn -main [dir]
  (doseq [[file content] (get-files dir)]
    (spit (str dir file) content)))

(comment
  (-main "target/pwa/")
  (-main "target/pwa-release/")
  (browse-url "http://localhost:4400"))

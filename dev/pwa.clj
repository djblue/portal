(ns pwa
  (:require [cheshire.core :as json]
            [clojure.java.browse :refer [browse-url]]
            [clojure.java.io :as io]
            [hiccup.core :refer [html]]
            [portal.colors :as c]))

(defn- manifest-json [settings]
  (json/generate-string
   {:short_name "Portal"
    :name (:name settings)
    :description "A clojure tool to navigate through your data."
    :icons
    [{:type "image/svg+xml"
      :sizes "512x512"
      :src "icon.svg"}]
    :scope (:host settings)
    :start_url (:host settings)
    :display "standalone"
    :display_override ["tabbed" "minimal-ui"]}
   {:pretty true}))

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

(defn- get-files [settings]
  (let [settings (merge settings (::c/nord c/themes))]
    {"index.html"     (index-html settings)
     "manifest.json"  (manifest-json settings)
     "icon.svg"       (slurp (io/resource "icon.svg"))
     "sw.js"          (slurp (io/resource "sw.js"))}))

(def envs
  {:dev  {:name "portal-dev"
          :dir "target/pwa/"
          :host "http://localhost:4400"}
   :prod {:name "portal"
          :dir "target/pwa-release/"
          :host "https://djblue.github.io/portal/"}})

(defn -main [env]
  (let [{:keys [dir] :as settings} (get envs (keyword env))]
    (doseq [[file content] (get-files settings)]
      (spit (str dir file) content))))

(comment
  (-main :dev)
  (-main :prod)
  (browse-url "http://localhost:4400"))

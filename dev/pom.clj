(ns pom
  (:require [hiccup.core :refer [html]]))

(defn- options->licenses [{:keys [license]}]
  [:licenses
   [:license
    [:name (:name license)]
    [:url  (:url license)]]])

(defn- options->scm [{:keys [url scm]}]
  [:scm [:url (:url scm url)] [:tag (:tag scm)]])

(defn- options->dependencies [options]
  [:dependencies
   (for [[dep {:mvn/keys [version]}]
         (merge {'org.clojure/clojure
                 {:mvn/version "1.10.3"}}
                (:deps options))]
     [:dependency
      [:groupId (namespace dep)]
      [:artifactId (name dep)]
      [:version version]])])

(defn- options->resources [options]
  [:resources
   (for [[directory {:keys [excludes target]}] (:resources options)]
     [:resource
      [:directory directory]
      (when (seq excludes)
        [:excludes
         (for [exclude excludes] [:exclude exclude])])
      (when target [:targetPath target])])])

(defn- options->repositories [options]
  [:repositories
   (for [[id {:keys [url]}] (:repos options)]
     [:repository [:id id] [:url url]])])

(defn- options->hiccup [options]
  (let [{:keys [lib version description url]} options]
    [:project
     {:xmlns "http://maven.apache.org/POM/4.0.0"
      :xmlns:xsi "http://www.w3.org/2001/XMLSchema-instance"
      :xsi:schemaLocation
      "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"}
     [:modelVersion "4.0.0"]

     [:groupId (namespace lib)]
     [:artifactId (name lib)]
     [:version version]
     [:name (name lib)]
     [:description description]
     [:url url]

     (options->licenses options)
     (options->scm options)
     (options->dependencies options)

     [:build
      (for [dir (:src-dirs options)]
        [:sourceDirectory dir])
      (options->resources options)]

     (options->repositories options)

     [:distributionManagement
      [:repository
       [:id "clojars"]
       [:name "Clojars repository"]
       [:url "https://clojars.org/repo"]]]]))

(defn- xml-str [hiccup]
  (str
   "<?xml version= \"1.0\" encoding= \"UTF-8\" ?>"
   (html {:mode :xml} hiccup)))

(defn generate [options]
  (-> options options->hiccup xml-str))

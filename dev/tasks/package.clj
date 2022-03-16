(ns tasks.package
  (:require [babashka.fs :as fs]
            [hiccup.core :refer [html]]
            [tasks.build :refer [build extensions]]
            [tasks.info :refer [options version]]
            [tasks.tools :refer [clj]]))

(defn- options->licenses [{:keys [license]}]
  [:licenses
   [:license
    [:name (:name license)]
    [:url  (:url license)]]])

(defn- options->scm [{:keys [url scm]}]
  [:scm [:url (:url scm url)] [:tag (:tag scm)]])

(defn- options->dependencies [options]
  [:dependencies
   (for [[dep {:mvn/keys [version] :keys [scope]}]
         (merge {'org.clojure/clojure
                 {:mvn/version "1.10.3"}}
                (:deps options))]
     [:dependency
      [:groupId (namespace dep)]
      [:artifactId (name dep)]
      [:version version]
      (when scope [:scope scope])])])

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

(defn pom-file [options]
  (let [lib     (:lib options)
        classes (:class-dir options)]
    (fs/path classes "META-INF/maven" (namespace lib) (name lib) "pom.xml")))

(defn pom []
  (let [target (pom-file options)]
    (when (seq
           (fs/modified-since
            target
            ["deps.edn"]))
      (println "=>" "writing" (str target))
      (fs/create-dirs (fs/parent target))
      (spit (str target) (generate options)))))

(defn jar
  "Build a jar."
  []
  (build)
  (pom)
  (when (seq
         (fs/modified-since
          (str "target/portal-" version ".jar")
          (concat
           ["pom.xml"]
           (fs/glob "src" "**")
           (fs/glob "resources" "**"))))
    (clj "-M:dev" "-m" :tasks.jar)))

(defn all
  "Package all release artifacts."
  []
  (jar)
  (extensions))

(defn -main [] (all))

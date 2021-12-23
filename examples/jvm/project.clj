(defproject portal-example-lein "1.0.0"
  :dependencies [[ring "1.8.2"]]
  :profiles
  {:dev {:source-paths ["dev"]
         :dependencies [[djblue/portal "0.19.0"]]}})

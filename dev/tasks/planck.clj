(ns tasks.planck
  (:require
   [tasks.tools :refer [sh]]))

(defn setup []
  (sh :sudo :add-apt-repository "ppa:mfikes/planck")
  (sh :sudo :apt-get :update)
  (sh :sudo :apt-get :install :planck))

(defn -main [] (setup))

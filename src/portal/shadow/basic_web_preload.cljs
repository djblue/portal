(ns portal.shadow.basic-web-preload
  (:require [portal.web :as p]))

;; Allows options to be propagated across page reloads
(p/set-defaults! {:theme :portal.colors/gruvbox})

(add-tap p/submit)

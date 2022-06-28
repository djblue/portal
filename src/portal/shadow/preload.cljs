(ns portal.shadow.preload
  (:require [portal.shadow.remote :as remote]))

(add-tap remote/submit)

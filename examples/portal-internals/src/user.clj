(ns user
  (:require [emmy.portal :as ep]
            [portal.internals.viewer :as internals]))

(ep/prepare!)
(internals/-main)
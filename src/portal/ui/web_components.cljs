(ns portal.ui.web-components
  (:require [portal.ui.macros :refer [defcomponent]]))

(defonce ^:private vs-code-api
  (when (exists? js/acquirevscodeapi)
    (js/acquirevscodeapi)))

(defn- post-message! [event]
  (let [message (.stringify js/JSON (clj->js event))]
    (if vs-code-api
      (.postMessage vs-code-api message "*")
      (.postMessage js/window.parent message "*"))))

(defn- set-header [header]
  (doseq [el (.querySelectorAll js/document "meta[name=theme-color]")]
    (.setAttribute el "content" header))
  (doseq [timeout (range 0 1000 250)]
    (js/setTimeout
     (fn []
       (post-message! {:type :set-theme :color ""})
       (post-message! {:type :set-theme :color header}))
     timeout)))

(defcomponent set-theme [header]
  (on-attribute-changed [_] (set-header header)))

(defn- element-visible? [element]
  (let [buffer 100
        rect   (.getBoundingClientRect element)
        height (or (.-innerHeight js/window)
                   (.. js/document -documentElement -clientHeight))
        width  (or (.-innerWidth js/window)
                   (.. js/document -documentElement -clientWidth))]
    (and (> (.-bottom rect) buffer)
         (< (.-top rect) (- height buffer))
         (> (.-right rect) buffer)
         (< (.-left rect) (- width buffer)))))

(defn- scroll-into-view* [element]
  (let [inline   (.getAttribute element "inline")
        block    (.getAttribute element "block")
        behavior (.getAttribute element "behavior")
        options  (cond-> {}
                   inline   (assoc :inline inline)
                   block    (assoc :block block)
                   behavior (assoc :behavior behavior))]
    (.scrollIntoView element (clj->js options))))

(defcomponent scroll-into-view [when]
  (on-connect [this]
    (if (= "not-visible" when)
      (when-not (element-visible? this)
        (scroll-into-view* this))
      (scroll-into-view* this))))
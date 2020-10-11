(ns portal.shortcuts
  (:require [clojure.string :as str]))

(defonce ^:private shortcuts (atom {}))

(defn- get-platform []
  (let [platform js/window.navigator.platform]
    (cond
      (#{"Macintosh" "MacIntel" "MacPPC" "Mac68K"} platform)  ::osx
      (#{"Win32" "Win64" "Windows" "WinCE"} platform)         ::windows
      (str/includes? platform "Linux")                        ::linux)))

(defn get-shortcut [definition]
  (or (get definition (get-platform))
      (get definition ::default)))

(defn- event->key [e]
  (.toLowerCase (.-key e)))

(defn- log->seq
  "Returns all key sequences in the event log."
  [log]
  (let [log (map event->key log)]
    (for [n (reverse (map inc (range (count log))))]
      (into [] (reverse (take n log))))))

(defn- log->combo
  "Return the last key combo from the event log."
  [log]
  (let [e (first log)]
    (cond-> #{(event->key e)}
      (.-ctrlKey e)  (conj "control")
      (.-metaKey e)  (conj "meta")
      (.-shiftKey e) (conj "shift")
      (.-altKey e)   (conj "alt"))))

(defonce ^:private log (atom (list)))

(defn- blur [] (reset! log (list)))

(defn- dispatch! [log]
  (doseq [combo (concat [(log->combo log)] (log->seq log))]
    (when-let [shortcut (get @shortcuts combo)]
      (blur)
      (shortcut))))

(defn- push-log! [event]
  (swap! log #(take 5 (conj % event))))

(defn- keydown [e]
  (dispatch! (push-log! e)))

(defonce ^:private init? (atom false))

(defn- init []
  (when-not @init?
    (reset! init? true)
    (js/window.addEventListener "blur" #(blur))
    (js/window.addEventListener "keydown" #(keydown %))))

(defn register! [definition f]
  (init)
  (when-let [combo (get-shortcut definition)]
    (swap! shortcuts assoc combo f)))


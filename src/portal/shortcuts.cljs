(ns portal.shortcuts
  (:require [clojure.string :as str]))

(defn- get-platform []
  (let [platform js/window.navigator.platform]
    (cond
      (#{"Macintosh" "MacIntel" "MacPPC" "Mac68K"} platform)  ::osx
      (#{"Win32" "Win64" "Windows" "WinCE"} platform)         ::windows
      (str/includes? platform "Linux")                        ::linux)))

(defn get-shortcut [definition]
  (cond
    (string? definition) #{definition}

    (map? definition)
    (or (get definition (get-platform))
        (get definition ::default))

    :else definition))

(defn- event->key [e]
  (when-let [k (.-key e)] (.toLowerCase k)))

(defn- log->seq
  "Returns all key sequences in the event log."
  [log]
  (let [log (map event->key log)]
    (for [n (reverse (map inc (range (count log))))]
      (into [] (reverse (take n log))))))

(defn- log->combo
  "Return the last key combo from the event log."
  [log]
  (when-let [e (first log)]
    (cond-> #{(event->key e)}
      (.-ctrlKey e)  (conj "control")
      (.-metaKey e)  (conj "meta")
      (.-shiftKey e) (conj "shift")
      (.-altKey e)   (conj "alt"))))

(defonce ^:private log (atom nil))

(defn clear! [] (reset! log (list)))

(defn match? [definition log]
  (some (fn [combo]
          (= combo (get-shortcut definition)))
        (concat [(log->combo log)] (log->seq log))))

(defn input? [log]
  (when-let [e (first log)]
    (#{"BUTTON" "INPUT" "SELECT"}
     (.. e -target -tagName))))

(defn- keydown [e] (swap! log #(take 5 (conj % e))))

(defn- init []
  (when (nil? @log)
    (clear!)
    (js/window.addEventListener "blur" #(clear!))
    (js/window.addEventListener "keydown" #(keydown %))))

(defn matched! [log]
  (clear!)
  (when-let [e (first log)] (.preventDefault e)))

(defn add! [k f]
  (init)
  (add-watch
   log k
   (fn [_ _ _ log]
     (when-not (empty? log)
       (f log)))))

(defn remove! [k] (remove-watch log k))


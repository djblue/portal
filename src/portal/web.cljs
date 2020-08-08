(ns portal.web
  (:require [portal.resources :as io]
            [portal.runtime :as rt]
            [portal.runtime.transit :as t]))

(defn- send! [msg]
  (js/Promise.
   (fn [resolve _reject]
     (let [body (t/json->edn msg)
           f    (get rt/ops (:op body))]
       (f body #(resolve (t/edn->json %)))))))

(defn- get-item [k]
  (js/window.localStorage.getItem k))

(defn- set-item [k v]
  (js/window.localStorage.setItem k v))

(defn- remove-item [k]
  (js/localStorage.removeItem k))

(defonce child-window (atom nil))
(defonce code (io/resource "main.js"))

(defn- init-child-window [document]
  (let [body   (.-body document)
        script (.createElement document "script")
        el     (.createElement document "div")]
    (set! (.-style body) "margin: 0")
    (set! (.-id el) "root")
    (set! (.-text script) code)
    (.appendChild body el)
    (.appendChild body script)))

(defn- open-inspector []
  (let [child  (js/window.open "" "portal", "resizable,scrollbars,status")
        doc    (.-document (.-window child))]
    (when-not (.-PORTAL_INIT child)
      (init-child-window doc)
      (set! (.-PORTAL_INIT child) true))
    (.portal.core.start_BANG_ child #'send!)
    (set! (.-onunload child)
          (fn []
            (remove-item ":portal/open")))
    (set! (.-onunload js/window)
          (fn []
            (when-not (.-closed child)
              (set-item ":portal/open" (js/Date.now)))))
    (reset! child-window child))
  true)

(defn- init []
  (when-let [string (get-item ":portal/open")]
    (if (< (- (js/Date.now) (js/parseInt string)) 5000)
      (open-inspector)
      (remove-item ":portal/open"))))

(defonce do-init (init))

(defn ^:export tap
  "Add portal as a tap> target."
  []
  (add-tap #'rt/update-value)
  nil)

(defn ^:export open
  "Open a new inspector window."
  []
  (open-inspector)
  nil)

(defn ^:export close
  "Close all current inspector windows."
  []
  (when-let [child @child-window]
    (.close child))
  nil)

(defn ^:export clear
  "Clear all values."
  []
  (rt/clear-values)
  nil)


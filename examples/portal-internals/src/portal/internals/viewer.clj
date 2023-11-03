(ns portal.internals.viewer
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [portal.api :as p]
            [portal.colors :as c]
            [portal.runtime :as rt]
            [portal.runtime.browser :as browser]
            [portal.runtime.fs :as fs]
            [portal.viewer :as v]))

(def default-portals (v/hiccup []))

(def portals (atom default-portals))

(defn- portal-web [id]
  (swap! portals
         (fn [instances]
           (cond-> instances
             (not (contains? (into #{} (map (comp :id second)) instances) id))
             (conj
              [:div
               {:id id
                :style {:flex "1"
                        :height "calc(100% - 2px)"
                        :border (str "1px solid "
                                     (get-in c/themes [::c/nord ::c/border]))
                        :border-top :none
                        :border-radius 2}}]))))
  nil)

(defmethod browser/-open ::iframe [{:keys [portal server]}]
  (let [src (str "http://" (:host server) ":" (:port server) "?" (:session-id portal))]
    (try
      (swap! portals
             conj
             (with-meta
               [:iframe
                {:src src
                 :style {:flex "1"
                         :height "100%"
                         :border (str "1px solid "
                                      (get-in c/themes [::c/nord ::c/border]))
                         :border-top :none
                         :border-radius 2}}]
               {:portal portal}))
      (catch Exception e (tap> e)))))

(defn close
  {:command true}
  ([]
   (reset! portals default-portals))
  ([portal]
   (reset! portals default-portals)))

(rt/register! #'close {:name 'portal.api/close})

(defn eval-str
  "Eval string as clojure code"
  {:command true}
  [s]
  (let [open p/open
        current-slide (some-> rt/*session* :options :value deref :current-slide)
        slide-ns (symbol (str "slide-" (inc current-slide)))]
    (with-redefs [p/open (fn open*
                           ([] (open* nil))
                           ([options]
                            (binding [rt/*session* nil]
                              (with-redefs [p/open open]
                                (open (assoc options :launcher ::iframe :editor :vs-code))))))]
      (binding [*ns* (create-ns slide-ns)]
        (refer-clojure)
        (eval (read-string (str "(do " s "\n)")))))))

(defn read-slides [path]
  (v/table
   (for [block (str/split (fs/slurp (io/resource path)) #"---\n")
         :let [[slide notes] (str/split block #"\+\+\+")]]
     {:slide
      (v/hiccup
       [:div
        {:style {:display :flex
                 :gap 20}}
        (v/markdown slide)])
      :notes
      (v/hiccup
       [:div
        {:style {:display :flex
                 :gap 20}}
        (v/markdown (str "# Notes\n" notes))])})
   {:columns [:slide :notes]}))

(defn prev-slide [presentation]
  (swap! @#'clojure.core/tapset empty)
  (reset! portals default-portals)
  (swap! presentation
         (fn [{:keys [current-slide] :as presentation}]
           (cond-> presentation
             (> current-slide 0)
             (update :current-slide dec)))))

(defn next-slide [presentation]
  (swap! @#'clojure.core/tapset empty)
  (reset! portals default-portals)
  (swap! presentation
         (fn [{:keys [slides current-slide] :as presentation}]
           (cond-> presentation
             (< (inc current-slide) (count slides))
             (update :current-slide inc)))))

(defn open [presentation]
  (let [instance (atom nil)]
    (reset!
     instance
     (p/inspect
      presentation
      {:mode :dev
       :window-title "Portal Internals"
       :on-load
       (fn []
         (p/eval-str @instance "(portal.ui.commands/toggle-shell portal.ui.state/state)")
         (p/eval-str @instance (fs/slurp (io/resource "portal/internals/viewer.cljs"))))}))))

(def presentation (atom nil))

(defn -main []
  (reset! presentation
          (v/default
           {:current-slide 0
            :portals portals
            :slides (read-slides "README.md")}
           ::slides))

  (p/register! #'eval-str)
  (p/register! #'portal.api/docs)
  (p/register! #'prev-slide)
  (p/register! #'next-slide)
  (p/register! #'portal-web)

  (open presentation))

(comment
  (-main)

  (defn start-watch [a path]
    (let [f (future
              (while true
                (swap! a assoc :slides (read-slides path))
                (Thread/sleep 1000)))]
      (fn stop-watch [] (future-cancel f))))

  (def stop (start-watch presentation "internals.md"))
  (stop)

  (prev-slide presentation)
  (next-slide presentation))
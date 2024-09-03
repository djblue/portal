(ns ^:no-doc portal.ui.viewer.source-location
  (:require [clojure.spec.alpha :as s]
            [portal.colors :as c]
            [portal.ui.inspector :as ins]
            [portal.ui.rpc :as rpc]
            [portal.ui.styled :as d]
            [portal.ui.theme :as theme]))

;;; :spec
(s/def ::ns symbol?)
(s/def ::column int?)
(s/def ::line int?)
(s/def ::file string?)

(s/def ::source-location
  (s/keys :req-un [::line ::column]
          :opt-un [::ns ::file]))
;;;

(defn- source-location? [value]
  (s/valid? ::source-location value))

(defn ->source-location [value]
  (when (source-location? value)
    (with-meta
      (select-keys value [:ns :column :line :file :label])
      (assoc (meta value)
             :portal.viewer/default :portal.viewer/source-location))))

(defn inspect-source [value]
  (let [theme (theme/use-theme)]
    [d/div
     {:on-click
      (fn [e]
        (.stopPropagation e)
        (rpc/call 'portal.runtime.jvm.editor/goto-definition value))
      :style/hover
      {:opacity 1
       :text-decoration :underline}
      :style
      {:opacity 0.75
       :cursor  :pointer
       :color   (::c/uri theme)}}
     [ins/highlight-words
      (str
       (or (:label value)
           (:ns value)
           (:file value))
       ":"
       (:line value))]]))

(def viewer
  {:predicate source-location?
   :component #'inspect-source
   :name :portal.viewer/source-location
   :doc "View a map as a source location, provides goto definition on click."})

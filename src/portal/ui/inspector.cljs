(ns portal.ui.inspector
  (:require [cognitect.transit :as t]
            [lambdaisland.deep-diff2.diff-impl :as diff]
            [portal.colors :as c]
            [portal.ui.lazy :as l]
            [portal.ui.styled :as s]))

(defn date? [value] (instance? js/Date value))
(defn url? [value] (instance? js/URL value))
(defn bin? [value] (instance? js/Uint8Array value))

(defn get-value-type [value]
  (cond
    (instance? diff/Deletion value)   :diff
    (instance? diff/Insertion value)  :diff
    (instance? diff/Mismatch value)   :diff

    (bin? value)      :binary

    (map? value)      :map
    (set? value)      :set
    (vector? value)   :vector
    (list? value)     :list
    (coll? value)     :coll
    (boolean? value)  :boolean
    (symbol? value)   :symbol
    (number? value)   :number
    (string? value)   :string
    (keyword? value)  :keyword
    (var? value)      :var

    (uuid? value)     :uuid
    (t/uuid? value)   :uuid
    (url? value)      :uri
    (t/uri? value)    :uri
    (date? value)     :date

    (t/tagged-value? value)
    (case (.-tag value)
      "portal.transit/var"        :var
      "portal.transit/object"     :object
      :tagged)))

(declare inspector)

(defn diff-added [settings value]
  (let [color (::c/diff-add settings)]
    [s/div
     {:style {:flex 1
              :background (str color "22")
              :border (str "1px solid " color)
              :border-radius (:border-radius settings)}}
     [inspector settings value]]))

(defn diff-removed [settings value]
  (let [color (::c/diff-remove settings)]
    [s/div
     {:style {:flex 1
              :background (str color "22")
              :border (str "1px solid " color)
              :border-radius (:border-radius settings)}}
     [inspector settings value]]))

(defn inspect-diff [settings value]
  (let [removed (get value :- ::not-found)
        added   (get value :+ ::not-found)]
    [s/div
     {:style {:display :flex :width "100%"}}
     (when-not (= removed ::not-found)
       [s/div {:style
               {:flex 1
                :margin-right
                (when-not (= added ::not-found)
                  (:spacing/padding settings))}}
        [diff-removed settings removed]])
     (when-not (= added ::not-found)
       [s/div {:style {:flex 1}}
        [diff-added settings added]])]))

(defn get-background [settings]
  (if (even? (:depth settings))
    (::c/background settings)
    (::c/background2 settings)))

(defn tagged-tag [settings tag]
  [s/div
   [s/span {:style {:color (::c/tag settings)}} "#"]
   tag])

(defn tagged-value [settings tag value]
  [s/div
   {:style {:display :flex :align-items :center}}
   [tagged-tag settings tag]
   [s/div {:style {:margin-left (:spacing/padding settings)}}
    [s/div
     {:style {:margin (* -1 (:spacing/padding settings))}}
     [inspector settings value]]]])

(defn preview-coll [open close]
  (fn [settings value]
    [s/div
     {:style
      {:color (::c/diff-remove settings)}}
     open
     (count value)
     (when (-> value meta :portal.runtime/more) "+")
     close]))

(def preview-map    (preview-coll "{" "}"))
(def preview-vector (preview-coll "[" "]"))
(def preview-list   (preview-coll "(" ")"))
(def preview-set    (preview-coll "#{" "}"))

(defn preview-object [settings value]
  [s/span {:style {:color (::c/text settings)}} (:type (.-rep value))])

(defn preview-tagged [settings value]
  [tagged-tag settings (.-tag value)])

(defn container-map [settings child]
  [s/div
   {:style
    {:width "100%"
     :display :grid
     :background (get-background settings)
     :grid-gap (:spacing/padding settings)
     :padding (:spacing/padding settings)
     :box-sizing :border-box
     :color (::c/text settings)
     :font-size  (:font-size settings)
     :border-radius (:border-radius settings)
     :border (str "1px solid " (::c/border settings))}} child])

(defn container-map-k [_settings child]
  [s/div {:style {:grid-column "1"
                  :display :flex
                  :align-items :center}} child])

(defn container-map-v [_settings child]
  [s/div {:style {:grid-column "2"
                  :display :flex
                  :align-items :center}} child])

(defn try-sort [values]
  (try (sort values)
       (catch :default _e values)))

(defn try-sort-map [values]
  (try (sort-by first values)
       (catch :default _e values)))

(defn inspect-map [settings values]
  [container-map
   settings
   [l/lazy-seq
    settings
    (for [[k v] (try-sort-map values)]
      [:<>
       {:key (hash k)}
       [container-map-k
        settings
        [inspector (assoc settings :coll values) k]]
       [container-map-v
        settings
        [inspector (assoc settings :coll values :k k) v]]])]])

(defn container-coll [settings child]
  [s/div
   {:style
    {:width "100%"
     :text-align :left
     :display :grid
     :background (get-background settings)
     :grid-gap (:spacing/padding settings)
     :padding (:spacing/padding settings)
     :box-sizing :border-box
     :color (::c/text settings)
     :font-size  (:font-size settings)
     :border-radius (:border-radius settings)
     :border (str "1px solid " (::c/border settings))}} child])

(defn inspect-coll [settings values]
  [container-coll
   settings
   [l/lazy-seq
    settings
    (map-indexed
     (fn [idx itm]
       ^{:key idx}
       [inspector (assoc settings :coll values :k idx) itm])
     values)]])

(defn trim-string [settings s]
  (let [max-length (:limits/string-length settings)]
    (if-not (> (count s) max-length)
      s
      (str (subs s 0 max-length) "..."))))

(defn inspect-number [settings value]
  [s/span {:style {:color (::c/number settings)}} value])

(defn hex-color? [s]
  (re-matches #"#[0-9a-f]{6}|#[0-9a-f]{3}gi" s))

(defn url-string? [s]
  (re-matches #"https?://.*" s))

(defn inspect-string [settings value]
  (cond
    (url-string? value)
    [s/span
     {:style {:color (::c/string settings)}}
     "\""
     [s/a
      {:href value
       :target "_blank"
       :style {:color (::c/string settings)}}
      (trim-string settings value)]
     "\""]

    (hex-color? value)
    [s/div
     {:style
      {:padding (* 0.65 (:spacing/padding settings))
       :box-sizing :border-box
       :background value}}
     [s/div
      {:style
       {:text-align :center
        :filter "contrast(500%) saturate(0) invert(1) contrast(500%)"
        :opacity 0.75
        :color value}}
      value]]

    :else
    [s/span {:style {:color (::c/string settings)}}
     (pr-str (trim-string settings value))]))

(defn inspect-namespace [settings value]
  (when-let [ns (namespace value)]
    [s/span {:style {:color (::c/namespace settings)}} ns "/"]))

(defn inspect-boolean [settings value]
  [s/span {:style {:color (::c/boolean settings)}}
   (pr-str value)])

(defn inspect-symbol [settings value]
  [s/span {:style {:color (::c/symbol settings) :white-space :nowrap}}
   [inspect-namespace settings value]
   (name value)])

(defn inspect-keyword [settings value]
  [s/span {:style {:color (::c/keyword settings) :white-space :nowrap}}
   ":"
   [inspect-namespace settings value]
   (name value)])

(defn inspect-date [settings value]
  [tagged-value settings "inst" (.toJSON value)])

(defn inspect-uuid [settings value]
  [tagged-value settings "uuid" (str value)])

(defn get-var-symbol [value]
  (cond
    (t/tagged-value? value) (.-rep value)
    :else                   (let [m (meta value)]
                              (symbol (name (:ns m)) (name (:name m))))))

(defn inspect-var [settings value]
  [s/span
   [s/span {:style {:color (::c/tag settings)}} "#'"]
   [inspect-symbol settings (get-var-symbol value)]])

(defn get-url-string [value]
  (cond
    (t/tagged-value? value) (.-rep value)
    :else                   (str value)))

(defn inspect-uri [settings value]
  (let [value (get-url-string value)]
    [s/a
     {:href value
      :style {:color (::c/uri settings)}
      :target "_blank"}
     value]))

(defn inspect-tagged [settings value]
  [tagged-value settings (.-tag value) (.-rep value)])

(defn inspect-default [settings value]
  [s/span {}
   (trim-string settings (pr-str value))])

(defn inspect-object [settings value]
  (let [value (.-rep value)]
    [s/span {:title (:type value)
             :style
             {:color (::c/text settings)}}
     (trim-string settings (:string value))]))

(defn get-preview-component [type]
  (case type
    :diff       inspect-diff
    :map        preview-map
    :set        preview-set
    :vector     preview-vector
    :list       preview-list
    :coll       preview-list
    :boolean    inspect-boolean
    :symbol     inspect-symbol
    :number     inspect-number
    :string     inspect-string
    :keyword    inspect-keyword
    :date       inspect-date
    :uuid       inspect-uuid
    :var        inspect-var
    :object     preview-object
    :uri        inspect-uri
    :tagged     preview-tagged
    inspect-default))

(def preview-type?
  #{:map :set :vector :list :coll :tagged :object})

(defn preview [settings value]
  (let [type (get-value-type value)
        component (get-preview-component type)]
    (when (preview-type? type)
      [component settings value])))

(defn get-inspect-component [type]
  (case type
    :diff       inspect-diff
    (:set :vector :list :coll) inspect-coll
    :map        inspect-map
    :boolean    inspect-boolean
    :symbol     inspect-symbol
    :number     inspect-number
    :string     inspect-string
    :keyword    inspect-keyword
    :date       inspect-date
    :uuid       inspect-uuid
    :var        inspect-var
    :object     inspect-object
    :uri        inspect-uri
    :tagged     inspect-tagged
    inspect-default))

(defn inspector [settings value]
  (let [preview? (> (:depth settings) (:limits/max-depth settings))
        component (or (get settings :component)
                      (let [type (get-value-type value)]
                        (if preview?
                          (get-preview-component type)
                          (get-inspect-component type))))
        settings (-> settings
                     (update :depth inc)
                     (dissoc :component)
                     (assoc :value value))
        nav-target? (= 2 (:depth settings))
        on-nav #((:portal/on-nav settings) (assoc settings :value value))]
    [s/div
     {:on-click
      (fn [e]
        (when nav-target?
          (.stopPropagation e)
          (on-nav)))
      :on-key-down
      (fn [e]
        (when (= (.-key e) "Enter")
          (.stopPropagation e)
          (on-nav)))
      :style {:cursor :pointer
              :width "100%"
              :padding (when (or preview? (not (coll? value)))
                         (* 0.65 (:spacing/padding settings)))
              :box-sizing :border-box
              :border-radius (:border-radius settings)
              :border "1px solid rgba(0,0,0,0)"}
      :tab-index (when nav-target? 0)
      :style/hover {:border
                    (when nav-target?
                      "1px solid #D8DEE9")}}
     [component settings value]]))


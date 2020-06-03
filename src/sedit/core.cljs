(ns sedit.core
  (:require [reagent.core :as r]
            [clojure.string :as s]
            [cognitect.transit :as t]))

(declare sedit)

(defn collapsible []
  (let [state (r/atom {:open? true})]
    (fn [props child]
      [:div
       {:on-click #(do
                     (swap! state update :open? not)
                     (.stopPropagation %))}
       (let [{:keys [open?]} @state]
         (if-not open?
           (:hidden props)
           child))])))

(defonce path (r/atom []))

(defn sedit-map [settings values]
  (let [[open close] ["{" "}"]
        settings (update settings :limits/max-depth dec)]
    (if (< (:limits/max-depth settings) 0)
      nil
      [:table
       {:key (hash values)
        :style
        {:border-collapse :collapse
         :color (:colors/text settings)
         :font-size  (:font-size settings)
         :border-radius (:border-radius settings)
         :border (str "1px solid " (:colors/border settings))}}

       [:thead
        [:tr
         {:style
          {:border-bottom (str "1px solid " (:colors/border settings))}}
         [:td
          (let []
            [:div {:style {:vertical-align :top
                           :box-sizing :border-box
                           :padding (:spacing/padding settings)
                           :color "#bf616a"}} open (count values) close])]
         [:td]]]
       [:tbody
        (take
         (:limits/max-length settings)
         (filter
          some?
          (for [[k v] values]
            (let [sedit-k (sedit settings k)
                  sedit-v (sedit (update settings :parent-path conj k) v)]
              (when (or (some? sedit-k) (some? sedit-v))
                [:tr
                 {:key (hash k)}
                 [:td {:on-click #(reset! path (conj (:parent-path settings) k))
                       :style
                       {:vertical-align :top
                        :cursor :pointer}}
                  sedit-k]
                 [:td {:style
                       {:vertical-align :top}}
                  sedit-v]])))))]])))

(defn sedit-coll [settings values]
  (let [[open close]
        (cond
          (vector? values)  ["[" "]"]
          (seq? values)     ["(" ")"]
          (set? values)     ["#{" "}"])
        settings (update settings :limits/max-depth dec)]
    (if (< (:limits/max-depth settings) 0)
      nil
      [:div
       {:key (hash values)
        :style
        {:display :flex
         :flex-direction (:layout/direction settings)
         :color (:colors/text settings)
         :font-size  (:font-size settings)
         :border-radius (:border-radius settings)
         :margin (:spacing/padding settings)
         :border (str "1px solid " (:colors/border settings))}}

       [:div
        {:style
         {:border-bottom (str "1px solid " (:colors/border settings))}}
        [:div
         [:div {:style {:vertical-align :top
                        :box-sizing :border-box
                        :padding (:spacing/padding settings)
                        :color "#bf616a"}} open (count values) close]]]
       (->> values
            (map-indexed
             (fn [idx itm]
               (sedit (update settings :parent-path conj idx) itm)))
            (filter some?)
            (take (:limits/max-length settings)))])))

(def deep-merge (partial merge-with merge))

(defn terminal [settings props & children]
  (into
   [:div (deep-merge
          {:key (hash children)
           :style {:padding (:spacing/padding settings)
                   :box-sizing :border-box}}
          props)]
   children))

(defn trim-string [settings s]
  (let [max-length (:limits/string-length settings)]
    (if-not (> (count s) max-length)
      s
      (str (subs s 0 max-length) "..."))))

(defn text-search [settings value]
  (cond
    (nil? (:input/text-search settings)) value

    (some? value)
    (let [text (:input/text-search settings)
          len  (count text)
          i    (s/index-of (s/lower-case value)
                           (s/lower-case text))]
      (when-not (nil? i)
        (let [before  (subs value 0 i)
              match   (subs value i (+ len i))
              after   (subs value (+ len i))]
          [:span before [:mark match] after])))))

(defn sedit [settings value]
  (cond
    (map? value)
    (sedit-map settings value)

    (coll? value)
    (sedit-coll settings value)

    (boolean? value)
    (terminal settings {:style {:color (:colors/boolean settings)}}
              (text-search settings (pr-str value)))

    (symbol? value)
    (terminal settings {:style {:color (:colors/symbol settings)}}
              (text-search settings value))

    (number? value)
    (terminal settings {:style {:color (:colors/number settings)}}
              (text-search settings value))

    (string? value)
    (terminal settings {:style {:color (:colors/string settings)}}
              (text-search settings (pr-str (trim-string settings value))))

    (keyword? value)
    (let [keyword-name (text-search settings (name value))
          keyword-namespace (text-search settings (namespace value))]
      (when keyword-name
        (terminal settings {:style {:color (:colors/keyword settings)}}
                  ":" (when keyword-namespace
                        [:span {:style {:color (:colors/keyword-namespace settings)}}
                         keyword-namespace
                         "/"])
                  keyword-name)))

    (instance? js/Date value)
    (terminal settings {:style {:color (:colors/date settings)}}
              (text-search settings  (pr-str value)))

    (instance? cljs.core/UUID value)
    (terminal settings {:style {:color (:colors/uuid settings)}}
              (text-search settings (pr-str value)))

    (instance? cljs.core/Var value)
    (terminal settings {:style {:color (:colors/var settings)}}
              (text-search settings (pr-str value)))

    :else
    (terminal settings {}
              (text-search settings (trim-string settings (pr-str value))))))

(def themes
  {:themes/nord
   {:colors/text "#d8dee9"
    :colors/background "#2e3440"
    :colors/boolean "#5e81ac"
    :colors/string "#a3be8c"
    :colors/keyword "#5e81ac"
    :colors/keyword-namespace "#88c0d0"
    :colors/symbol "#d8dee9"
    :colors/number "#b48ead"
    :colors/date "#ebcb8b"
    :colors/uuid "#d08770"
    :colors/var "#88c0d0"
    :colors/border "#4c566a"}})

(def default-settings
  (merge
   {:font/family "monospace"
    :font-size "12pt"
    :limits/string-length 100
    :limits/max-depth 3
    :limits/max-length 1000
    :layout/direction :row
    :spacing/padding "3px"
    :border-radius "2px"}
   (:themes/nord themes)))

(def example
  {:example/booleans #{true false}
   :example/nil nil
   :example/vector [1 2 4]
   "string-key" "string-value"
   :example/list (list 1 2 3)
   :example/set #{1 2 3}
   {:example/settings default-settings} :hello-world
   #{1 2 3} [4 5 6]
   :example/date (js/Date.)
   :example/var #'default-settings
   :example/uuid (random-uuid)
   :example/nested-vector [1 2 3 [4 5 6]]
   :example/code '(defn hello-world [] (println "hello, world"))})

(defonce state (r/atom default-settings))

(defn app []
  (let [settings    @state
        value       (:sedit/value settings)
        parent-path @path
        settings    (assoc settings :parent-path parent-path)]
    [:div
     {:style
      {:display :flex
       :justify-content :space-between
       :flex-direction :column
       :background (:colors/background settings)
       :color (:colors/text settings)
       :font-family (:font/family settings)
       :font-size (:font-size settings)
       :height "100vh"
       :width "100vw"}}

     [:div
      {:style
       {:height "64px"
        :flex-direction :row
        :display :flex
        :align-items :center
        :border-bottom  (str "1px solid " (:colors/border settings))}}
      [:button
       {:on-click #(swap! path (fn [v] (if (empty? v) v (pop v))))
        :style
        {:background (:colors/text settings)
         :color (:colors/background settings)
         :font-size (:font-size settings)
         :border :none
         :box-sizing :border-box
         :padding "10px 20px"
         :border-radius (:border-radius settings)
         :cursor :pointer
         :margin "0 20px"}} "back"]]
     [:div
      {:style
       {:display :flex
        :flex 1
        :align-items :center
        :justify-content :center
        :overflow :auto}}
      [:div {:style {:max-height "100%" :max-width "100%"}}
       [:div {:style {:padding "64px" :box-sizing :border-box}}
        (sedit settings (get-in value parent-path))]]]

     (sedit (-> settings
                (dissoc :input/text-search)
                (assoc :layout/direction :row))
            (select-keys
             settings
             [:limits/string-length
              :limits/max-depth
              :limits/max-length
              :parent-path]))]))

(defn json->edn [json]
  (let [r (t/reader :json)]
    (t/read r json)))

(defn edn->json [edn]
  (let [w (t/writer :json)]
    (t/write w edn)))

(defn send-rpc!
  ([msg] (send-rpc! msg identity))
  ([msg done]
   (-> (js/fetch
        "/rpc"
        #js {:method "POST" :body (edn->json msg)})
       (.then #(.text %))
       (.then json->edn)
       (.then done))))

(def load-state
  (partial send-rpc! {:op :sedit.rpc/load-state} #(swap! state merge %)))

(def await-state!
  (partial send-rpc! {:op :sedit.rpc/await-state} #(swap! state merge %)))

(defn promise-loop [f]
  (.finally (f) #(promise-loop f)))

(defn main! []
  (load-state)
  (promise-loop await-state!)
  (r/render [app]
            (.getElementById js/document "root")))

(defn reload! [] (main!))

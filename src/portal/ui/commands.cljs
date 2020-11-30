(ns portal.ui.commands
  (:require [clojure.pprint :as pp]
            [clojure.set :as set]
            [clojure.string :as str]
            [portal.async :as a]
            [portal.colors :as c]
            [portal.shortcuts :as shortcuts]
            [portal.ui.inspector :as ins :refer [inspector]]
            [portal.ui.state :as st :refer [tap-state state]]
            [portal.ui.styled :as s]
            [reagent.core :as r]))

(defn copy-to-clipboard! [s]
  (let [el (js/document.createElement "textarea")]
    (set! (.-value el) s)
    (js/document.body.appendChild el)
    (.select el)
    (js/document.execCommand "copy")
    (js/document.body.removeChild el)))

(defn copy-edn! [value]
  (copy-to-clipboard! (with-out-str (pp/pprint value))))

(defonce input (r/atom nil))

(defn close [] (reset! input nil))

(defn pop-up [_ & children]
  (into
   [s/div
    {:on-click close
     :style
     {:position :fixed
      :top 0
      :left 0
      :right 0
      :bottom 0
      :z-index 100
      :padding-top 200
      :padding-bottom 200
      :box-sizing :border-box
      :height "100%"
      :overflow :hidden}}]
   children))

(defn container [settings & children]
  (into
   [s/div
    {:style
     {:width "80%"
      :max-height "40vh"
      :margin "0 auto"
      :overflow :hidden
      :display :flex
      :flex-direction :column
      :background (::c/background2 settings)
      :box-shadow "0 0 10px #0007"
      :border (str "1px solid " (::c/border settings))
      :border-radius (:border-radius settings)}}]
   children))

(defn with-shortcuts [f]
  (let [k (gensym)]
    (r/create-class
     {:component-did-mount
      (fn []
        (shortcuts/add! k f))
      :component-will-receive-props
      (fn [_this [_ f]]
        (shortcuts/add! k f))
      :component-will-unmount
      (fn []
        (shortcuts/remove! k))
      :reagent-render (constantly nil)})))

(defn checkbox [settings checked?]
  [s/div
   {:style
    {:width "1.25em"
     :height "1.25em"
     :position :relative
     :display :flex
     :align-items :center
     :justify-content :center}}
   [s/div
    {:style
     {:width "0.75em"
      :position :absolute
      :height "0.75em"
      :border (str "2px solid " (::c/string settings))
      :border-radius "50%"}}]
   (when checked?
     [s/div
      {:style
       {:width "0.5em"
        :height "0.5em"
        :position :absolute
        :background (::c/string settings)
        :border-radius "50%"}}])])

(defn scroll-into-view []
  (let [el (atom nil)]
    (fn []
      (r/create-class
       {:component-did-mount
        (fn []
          (when-let [el @el] (.scrollIntoView el #js {:block "center"})))
        :reagent-render
        (fn [] [:div {:ref #(reset! el %) :style {:height "100%"}}])}))))

(defn selector-component []
  (let [selected (r/atom #{})
        active   (r/atom 0)]
    (fn [settings input]
      [container
       settings
       [s/div
        {:style {:box-sizing :border-box
                 :padding (:spacing/padding settings)}}
        "(Press <space> to select, <a> to toggle all, <i> to invert selection)"]
       (let [selected? @selected
             on-done   (:run input)
             options   (:options input)
             n         (count (:options input))
             on-select #(swap! selected (if (selected? %) disj conj) %)]
         [:<>
          [with-shortcuts
           (fn [log]
             (when
              (condp shortcuts/match? log
                "arrowup"   (swap! active #(mod (dec %) n))
                "arrowdown" (swap! active #(mod (inc %) n))

                "a"
                (if (= (count @selected) (count options))
                  (reset! selected #{})
                  (swap! selected into options))

                "i" (swap! selected #(set/difference (into #{} options) %))
                " " (on-select (nth options @active))
                "enter" (on-done @selected)

                nil)
               (shortcuts/matched! log)))]
          [s/div
           {:style {:overflow :auto}}
           (->> options
                (map-indexed
                 (fn [index option]
                   (let [active? (= index @active)]
                     [s/div
                      {:key (hash option)
                       :on-click (fn [e]
                                   (.stopPropagation e)
                                   (on-select option))
                       :style
                       (merge
                        {:border-left (str "5px solid #0000")
                         :padding-left (:spacing/padding settings)
                         :cursor :pointer
                         :box-sizing :border-box
                         :color (if (selected? option)
                                  (::c/boolean settings)
                                  (::c/text settings))
                         :display :flex
                         :align-items :center}
                        (when active?
                          {:border-left (str "5px solid " (::c/boolean settings))
                           :background (::c/background settings)}))}
                      (when active? [scroll-into-view])
                      [checkbox settings (some? (selected? option))]
                      [inspector settings option]])))
                doall)]])])))

(def shortcut->symbol
  {"arrowright" "⭢"
   "arrowleft" "⭠"
   "arrowup" "⭡"
   "arrowdown" "⭣"})

(defn shortcut [settings command]
  (when-let [combo (shortcuts/get-shortcut command)]
    [s/div {:style
            {:display :flex
             :align-items :center
             :white-space :nowrap}}
     (for [k combo]
       ^{:key k}
       [s/div {:style
               {:background "#0002"
                :border-radius (:border-radius settings)
                :padding-top (* 0.5 (:spacing/padding settings))
                :padding-bottom (* 0.5 (:spacing/padding settings))
                :padding-left (:spacing/padding settings)
                :padding-right (:spacing/padding settings)
                :margin-right  (:spacing/padding settings)}}
        (get shortcut->symbol k (.toUpperCase k))])]))

(defn palette-component []
  (let [active (r/atom 0)
        filter-text (r/atom "")]
    (fn [settings args]
      (let [options
            (remove
             (fn [option]
               (let [text @filter-text]
                 (or
                  (when-not (str/blank? text)
                    (not (str/includes? (pr-str (:name option)) text))))))
             (:options args))
            n (count options)
            on-close close
            on-select
            (fn []
              (reset! filter-text "")
              (on-close)
              (when-let [option (nth options @active)]
                ((:on-select args) option)))]
        [container
         settings
         [s/div
          {:style
           {:padding (:spacing/padding settings)
            :border-bottom (str "1px solid " (::c/border settings))}}
          [with-shortcuts
           (fn [log]
             (when
              (condp shortcuts/match? log
                "arrowup"   (swap! active #(mod (dec %) n))
                "arrowdown" (swap! active #(mod (inc %) n))
                "enter"     (on-select)
                nil)
               (shortcuts/matched! log)))]
          [s/input
           {:auto-focus true
            :value @filter-text
            :on-change #(do
                          (reset! active 0)
                          (reset! filter-text (.-value (.-target %))))
            :style
            {:width "100%"
             :background (::c/background settings)
             :padding (:spacing/padding settings)
             :box-sizing :border-box
             :font-size (:font-size settings)
             :color (::c/text settings)
             :border (str "1px solid " (::c/border settings))}}]]
         [s/div
          {:style
           {:height "100%"
            :overflow :auto}}
          (->> options
               (map-indexed
                (fn [index command]
                  (let [active? (= index @active)]
                    ^{:key index}
                    [s/div
                     {:on-click #(do
                                   (.stopPropagation %)
                                   (reset! active index)
                                   (on-select))
                      :style
                      (merge
                       {:border-left (str "5px solid #0000")
                        :cursor :pointer
                        :display :flex
                        :justify-content :space-between
                        :align-items :center}
                       (when active?
                         {:border-left (str "5px solid " (::c/boolean settings))
                          :background (::c/background settings)}))
                      :style/hover
                      {:background (::c/background settings)}}
                     (when active? [scroll-into-view])
                     [inspector settings (:name command)]
                     [shortcut settings command]])))
               doall)]]))))

(defn coll-keys [value]
  (into [] (set (mapcat keys value))))

(defn coll-of-maps [settings]
  (let [value (:portal/value settings)]
    (and (not (map? value))
         (coll? value)
         (every? map? value))))

(defn map-keys [value]
  (coll-keys (vals value)))

(defn map-of-maps [settings]
  (let [value (:portal/value settings)]
    (and (map? value)
         (every? map? (vals value)))))

(defn transpose-map [value]
  (reduce
   (fn [m path]
     (assoc-in m (reverse path) (get-in value path)))
   {}
   (for [row (keys value)
         column (map-keys value)
         :when (contains? (get value row) column)]
     [row column])))

(defn get-functions [settings]
  (a/let [v      (:portal/value settings)
          invoke (:portal/on-invoke settings)
          fns    (invoke 'portal.runtime/get-functions v)]
    (for [f fns]
      {:name f
       :run (fn [_settings]
              (a/let [result (invoke f v)]
                (st/push f result)))})))

(def commands
  [{:name 'clojure.core/vals
    :predicate (comp map? :portal/value)
    :run (fn [settings]
           (let [v (:portal/value settings)]
             (when (map? v)
               (st/push ::keys (vals v)))))}
   {:name 'clojure.core/keys
    :predicate (comp map? :portal/value)
    :run (fn [settings]
           (let [v (:portal/value settings)]
             (when (map? v)
               (st/push ::keys (keys v)))))}
   {:name 'clojure.core/count
    :predicate (comp coll? :portal/value)
    :run (fn [settings]
           (let [v (:portal/value settings)]
             (when (coll? v)
               (st/push :count (count v)))))}
   {:name 'clojure.core/first
    :predicate (comp coll? :portal/value)
    :run (fn [settings]
           (let [v (:portal/value settings)]
             (when (coll? v)
               (st/push :get (first v)))))}
   {:name 'clojure.core/rest
    :predicate (comp coll? :portal/value)
    :run (fn [settings]
           (let [v (:portal/value settings)]
             (when (coll? v)
               (st/push :get (rest v)))))}
   {:name 'clojure.core/get
    :predicate (comp map? :portal/value)
    :run (fn [settings]
           (let [v (:portal/value settings)]
             (when (map? v)
               (reset! input
                       {:component #(-> [pop-up %1 [palette-component %1 %2]])
                        :options (map #(-> {:name %}) (keys v))
                        :on-select
                        (fn [option]
                          (st/push :get (get v (:name option))))}))))}
   {:name 'clojure.core/get-in
    :predicate (comp map? :portal/value)
    :run (fn [settings]
           (let [get-key
                 (fn get-key [path v]
                   (when (map? v)
                     (reset! input
                             {:component #(-> [pop-up %1 [palette-component %1 %2]])
                              :options (map #(-> {:name %}) (concat [::done] (keys v)))
                              :on-select
                              (fn [option]
                                (let [k (:name option)
                                      path (conj path k)
                                      next-value (get v k)]
                                  (cond
                                    (= k ::done)
                                    (st/push :get-in v)

                                    (not (map? next-value))
                                    (st/push :get-in next-value)

                                    :else
                                    (get-key path next-value))))})))]
             (get-key [] (:portal/value settings))))}
   {:name 'clojure.core/select-keys
    :predicate (comp map? :portal/value)
    :run (fn [settings]
           (let [v (:portal/value settings)]
             (when (map? v)
               (reset! input
                       {:component #(-> [pop-up %1 [selector-component %1 %2]])
                        :options (keys v)
                        :run
                        (fn [options]
                          (close)
                          (st/push
                           :select-keys
                           (select-keys v options)))}))))}
   {:name :portal.data/select-columns
    :predicate coll-of-maps
    :run (fn [settings]
           (let [v (:portal/value settings)]
             (when (coll? v)
               (reset! input
                       {:component #(-> [pop-up %1 [selector-component %1 %2]])
                        :options (coll-keys v)
                        :run
                        (fn [options]
                          (close)
                          (st/push
                           :select-columns
                           (map #(select-keys % options) v)))}))))}
   {:name :portal.data/select-columns
    :predicate map-of-maps
    :run (fn [settings]
           (let [v (:portal/value settings)]
             (reset! input
                     {:component #(-> [pop-up %1 [selector-component %1 %2]])
                      :options (map-keys v)
                      :run
                      (fn [options]
                        (close)
                        (st/push
                         :select-columns
                         (reduce-kv
                          (fn [v k m]
                            (assoc v k (select-keys m options)))
                          v
                          v)))})))}
   {:name :portal.data/transpose-map
    :predicate map-of-maps
    :run (fn [settings]
           (st/push
            :transpose-map
            (transpose-map (:portal/value settings))))}
   {:name :portal.command/close-command-palette
    ::shortcuts/osx ["escape"]
    ::shortcuts/default ["escape"]
    :run close}
   {:name :portal.command/open-command-palette
    ::shortcuts/osx #{"meta" "shift" "p"}
    ::shortcuts/default #{"control" "shift" "p"}
    :run (fn [settings]
           (a/let [fns (get-functions settings)
                   commands (remove
                             (fn [option]
                               (or
                                (#{:portal.command/close-command-palette
                                   :portal.command/open-command-palette}
                                 (:name option))
                                (when-let [predicate (:predicate option)]
                                  (not (predicate settings)))))
                             (concat fns commands (:commands settings)))]
             (reset! input {:component #(-> [pop-up %1 [palette-component %1 %2]])
                            :options commands
                            :on-select
                            (fn [option]
                              ((:run option) settings))})))}
   {:name :portal.command/theme-solarized-dark
    :run (fn [_settings] (st/set-theme! ::c/solarized-dark))}
   {:name :portal.command/theme-solarized-light
    :run (fn [_settings] (st/set-theme! ::c/solarized-light))}
   {:name :portal.command/theme-nord
    :run (fn [_settings] (st/set-theme! ::c/nord))}
   {:name :portal.command/copy-as-edn
    ::shortcuts/osx #{"meta" "c"}
    ::shortcuts/default #{"control" "c"}
    :run
    (fn [settings]
      (let [datafy (:datafy settings)
            value (:portal/value (merge @tap-state @state))]
        (copy-edn! (datafy value))))}
   {:name :portal.command/copy-path
    ::shortcuts/default #{"shift" "c"}
    :run (fn [_settings]
           (copy-edn! (st/get-path @state)))}
   {:name :portal.command/history-back
    ::shortcuts/osx #{"meta" "arrowleft"}
    ::shortcuts/default #{"control" "arrowleft"}
    :run (fn [settings] ((:portal/on-back settings)))}
   {:name :portal.command/history-forward
    ::shortcuts/osx #{"meta" "arrowright"}
    ::shortcuts/default #{"control" "arrowright"}
    :run (fn [settings] ((:portal/on-forward settings)))}
   {:name :portal.command/history-first
    ::shortcuts/osx #{"meta" "shift" "arrowleft"}
    ::shortcuts/default #{"control" "shift" "arrowleft"}
    :run (fn [settings] ((:portal/on-first settings)))}
   {:name :portal.command/history-last
    ::shortcuts/osx #{"meta" "shift" "arrowright"}
    ::shortcuts/default #{"control" "shift" "arrowright"}
    :run (fn [settings] ((:portal/on-last settings)))}
   {:name :portal.command/clear
    ::shortcuts/default #{"control" "l"}
    :run (fn [settings]
           ((:portal/on-clear settings)))}])

(defn palette [settings _value]
  [:<>
   [with-shortcuts
    (fn [log]
      (doseq [command commands]
        (when (shortcuts/match? command log)
          (shortcuts/matched! log)
          ((:run command) settings))))]
   (when-let [{:keys [component] :as args} @input]
     [component settings args])])

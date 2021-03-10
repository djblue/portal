(ns portal.ui.commands
  (:require ["react" :as react]
            [clojure.pprint :as pp]
            [clojure.set :as set]
            [clojure.string :as str]
            [portal.async :as a]
            [portal.colors :as c]
            [portal.shortcuts :as shortcuts]
            [portal.ui.inspector :as ins]
            [portal.ui.state :as state]
            [portal.ui.styled :as s]
            [portal.ui.theme :as theme]
            [reagent.core :as r]))

(defonce input (r/atom nil))

(defn open [f] (reset! input f))
(defn close [] (reset! input nil))

(defn container [& children]
  (let [theme (theme/use-theme)]
    (into
     [s/div
      {:on-click #(.stopPropagation %)
       :style
       {:width "80%"
        :max-height "40vh"
        :height :fit-content
        :margin "0 auto"
        :overflow :hidden
        :display :flex
        :flex-direction :column
        :background (::c/background2 theme)
        :box-shadow "0 0 10px #0007"
        :border [1 :solid (::c/border theme)]
        :border-radius (:border-radius theme)}}]
     children)))

(defn- use-shortcuts [k f]
  (react/useEffect
   (fn []
     (shortcuts/add! k f)
     (fn []
       (shortcuts/remove! k)))
   #js [f]))

(defn checkbox [checked?]
  (let [theme (theme/use-theme)]
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
        :border [2 :solid (::c/string theme)]
        :border-radius "50%"}}]
     (when checked?
       [s/div
        {:style
         {:width "0.5em"
          :height "0.5em"
          :position :absolute
          :background (::c/string theme)
          :border-radius "50%"}}])]))

(defn- scroll-into-view []
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
    (fn [input]
      (let [theme     (theme/use-theme)
            selected? @selected
            on-done   (:run input)
            options   (:options input)
            n         (count (:options input))
            on-select #(swap! selected (if (selected? %) disj conj) %)]
        (use-shortcuts
         ::selector
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
             (shortcuts/matched! log))))

        [container
         [s/div
          {:style {:box-sizing :border-box
                   :padding (:spacing/padding theme)}}
          "(Press <space> to select, <a> to toggle all, <i> to invert selection)"]
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
                       {:border-left [5 :solid "#0000"]
                        :padding-left (:spacing/padding theme)
                        :cursor :pointer
                        :box-sizing :border-box
                        :color (if (selected? option)
                                 (::c/boolean theme)
                                 (::c/text theme))
                        :display :flex
                        :align-items :center
                        :height :fit-content}
                       (when active?
                         {:border-left [5 :solid (::c/boolean theme)]
                          :background (::c/background theme)}))}
                     (when active? [scroll-into-view])
                     [checkbox (some? (selected? option))]
                     [ins/inspector option]])))
               doall)]]))))

(def shortcut->symbol
  {"arrowright" "⭢"
   "arrowleft" "⭠"
   "arrowup" "⭡"
   "arrowdown" "⭣"})

(defn combo-order [k]
  (get {"control" 0 "meta" 1 "shift" 2 "alt" 3} k 4))

(defn shortcut [command]
  (let [theme (theme/use-theme)]
    (when-let [combo (shortcuts/get-shortcut command)]
      [s/div {:style
              {:display :flex
               :align-items :center
               :white-space :nowrap}}
       (for [k (sort-by combo-order combo)]
         ^{:key k}
         [s/div {:style
                 {:background "#0002"
                  :border-radius (:border-radius theme)
                  :padding-top (* 0.5 (:spacing/padding theme))
                  :padding-bottom (* 0.5 (:spacing/padding theme))
                  :padding-left (:spacing/padding theme)
                  :padding-right (:spacing/padding theme)
                  :margin-right  (:spacing/padding theme)}}
          (get shortcut->symbol k (.toUpperCase k))])])))

(defn palette-component-item [props & children]
  (let [theme (theme/use-theme)
        {::keys [active? on-click]} props]
    (into
     [s/div
      {:on-click on-click
       :style
       (merge
        {:border-left [5 :solid "#0000"]
         :cursor :pointer
         :display :flex
         :justify-content :space-between
         :align-items :center
         :height :fit-content}
        (when active?
          {:border-left [5 :solid (::c/boolean theme)]
           :background (::c/background theme)}))
       :style/hover
       {:background (::c/background theme)}}]
     children)))

(defn palette-component []
  (let [active (r/atom 0)
        filter-text (r/atom "")]
    (fn [{:keys [on-select options]}]
      (let [theme (theme/use-theme)
            text @filter-text
            options
            (keep
             (fn [option]
               (when (or (str/blank? text)
                         (str/includes? (pr-str (::value (second option))) text))
                 option))
             options)
            n (count options)
            on-close close
            on-select
            (fn []
              (reset! filter-text "")
              (on-close)
              (when-let [option (nth options @active)]
                (on-select (second option))))]
        (use-shortcuts
         ::palette
         (fn [log]
           (when
            (condp shortcuts/match? log
              "arrowup"   (swap! active #(mod (dec %) n))
              "arrowdown" (swap! active #(mod (inc %) n))
              "enter"     (on-select)
              nil)
             (shortcuts/matched! log))))
        [container
         [s/div
          {:style
           {:padding (:spacing/padding theme)
            :border-bottom [1 :solid (::c/border theme)]}}

          [s/input
           {:auto-focus true
            :value @filter-text
            :on-change #(do
                          (reset! active 0)
                          (reset! filter-text (.-value (.-target %))))
            :style
            {:width "100%"
             :background (::c/background theme)
             :padding (:spacing/padding theme)
             :box-sizing :border-box
             :font-size (:font-size theme)
             :color (::c/text theme)
             :border [1 :solid (::c/border theme)]}}]]
         [s/div
          {:style
           {:height "100%"
            :overflow :auto}}
          (->> options
               (map-indexed
                (fn [index option]
                  (let [active? (= index @active)
                        on-click (fn [e]
                                   (.stopPropagation e)
                                   (reset! active index)
                                   (on-select))]
                    ^{:key index}
                    [:<>
                     (when active? [scroll-into-view])
                     (update option 1 assoc
                             ::active? active?
                             ::on-click on-click)])))
               doall)]]))))

(defn- empty-args [_] nil)

(defn fn->command [f]
  (fn [state]
    (when-let [selected (state/get-selected-context @state)]
      (state/dispatch! state f selected))))

(defn make-command [{:keys [name predicate args f] :as opts}]
  (let [predicate (or predicate (constantly true))]
    (merge
     (select-keys opts [::shortcuts/default :shortcuts/osx])
     {:name name
      :predicate (comp predicate state/get-selected-value)
      :run (fn [state]
             (a/let [v      (state/get-selected-value @state)
                     args   ((or args empty-args) v)
                     result (apply f v args)]
               (when (predicate v)
                 (state/dispatch!
                  state
                  state/history-push
                  {:portal/key name
                   :portal/f f
                   :portal/args args
                   :portal/value result}))))})))

(defn get-functions [state]
  (a/let [v   (state/get-selected-value @state)
          fns (state/invoke 'portal.runtime/get-functions v)]
    (for [f fns]
      (make-command
       {:name f
        :f
        (if-not (= f 'clojure.datafy/nav)
          #(state/invoke f %)
          #(when-let [args (state/get-nav-args @state)]
             (apply state/invoke f args)))}))))

(declare commands)

(def open-command-palette
  {:name :portal.command/open-command-palette
   :label "Show All Commands"
   ::shortcuts/osx #{"meta" "shift" "p"}
   ::shortcuts/default #{"control" "shift" "p"}
   :run (fn [state]
          (a/let [fns (get-functions state)
                  commands (remove
                            (fn [option]
                              (or
                               (#{:portal.command/close-command-palette
                                  :portal.command/open-command-palette}
                                (:name option))
                               (when-let [predicate (:predicate option)]
                                 (not (predicate @state)))))
                            (concat fns commands (:commands @state)))]
            (open
             (fn [state]
               [palette-component
                {:on-select
                 (fn [option]
                   ((:run option) state))
                 :options
                 (for [command commands]
                   [palette-component-item
                    {:run (:run command)
                     ::value (:name command)}
                    [ins/inspector (:name command)]
                    [shortcut command]])}]))))})

;; pick args

(defn pick-one [options]
  (js/Promise.
   (fn [resolve]
     (open
      (fn [_state]
        [palette-component
         {:on-select #(resolve [(::value %)])
          :options
          (for [option options]
            [palette-component-item
             {::value option}
             [ins/inspector option]])}])))))

(defn pick-many [options]
  (js/Promise.
   (fn [resolve]
     (open
      (fn []
        [selector-component
         {:options options
          :run
          (fn [options]
            (close)
            (resolve [options]))}])))))

(defn pick-in [v]
  (js/Promise.
   (fn [resolve]
     (let [get-key
           (fn get-key [path v]
             (open
              (fn [_state]
                [palette-component
                 {:on-select
                  (fn [option]
                    (let [k (::value option)
                          path (conj path k)
                          next-value (get v k)]
                      (cond
                        (= k ::done)
                        (resolve [(drop-last path)])

                        (not (map? next-value))
                        (resolve [path])

                        :else
                        (get-key path next-value))))
                  :options
                  (for [k (concat [::done] (keys v))]
                    [palette-component-item
                     {::value k}
                     [ins/inspector k]])}])))]
       (get-key [] v)))))

;; clojure commands

(def clojure-commands
  [{:f vals
    :predicate map?
    :name 'clojure.core/vals}
   {:f keys
    :predicate map?
    :name 'clojure.core/keys}
   {:f count
    :predicate #(or (coll? %) (string? %))
    :name 'clojure.core/count}
   {:f first
    :predicate coll?
    :name 'clojure.core/first}
   {:f rest
    :predicate coll?
    :name 'clojure.core/rest}
   {:f get
    :predicate map?
    :args (comp pick-one keys)
    :name 'clojure.core/get}
   {:f get-in
    :predicate map?
    :args pick-in
    :name 'clojure.core/get-in}
   {:f select-keys
    :predicate map?
    :args (comp pick-many keys)
    :name 'clojure.core/select-keys}])

;; portal data commands

(defn coll-of-maps [value]
  (and (not (map? value))
       (coll? value)
       (every? map? value)))

(defn map-of-maps [value]
  (and (map? value) (every? map? (vals value))))

(defn coll-keys [value]
  (into [] (set (mapcat keys value))))

(defn map-keys [value]
  (coll-keys (vals value)))

(defn transpose-map [value]
  (reduce
   (fn [m path]
     (assoc-in m (reverse path) (get-in value path)))
   {}
   (for [row (keys value)
         column (map-keys value)
         :when (contains? (get value row) column)]
     [row column])))

(defn select-columns [value ks]
  (cond
    (map? value)
    (reduce-kv
     (fn [v k m]
       (assoc v k (select-keys m ks)))
     value
     value)
    :else (map #(select-keys % ks) value)))

(def portal-data-commands
  [{:f transpose-map
    :predicate map-of-maps
    :name 'portal.data/transpose-map}
   {:f select-columns
    :predicate coll-of-maps
    :args (comp pick-many coll-keys)
    :name 'portal.data/select-columns}
   {:f select-columns
    :predicate map-of-maps
    :args (comp pick-many map-keys)
    :name 'portal.data/select-columns}])

(defn- copy-to-clipboard! [s]
  (let [el (js/document.createElement "textarea")]
    (set! (.-value el) s)
    (js/document.body.appendChild el)
    (.select el)
    (js/document.execCommand "copy")
    (js/document.body.removeChild el)))

(defn copy-edn! [value]
  (copy-to-clipboard!
   (with-out-str
     (binding [*print-meta* true ;; TODO: doesn't work
               *print-length* 1000
               *print-level* 100]
       (pp/pprint value)))))

(def portal-commands
  [{:name :portal.command/focus-selected
    ::shortcuts/default #{"enter"}
    :run (fn->command state/focus-selected)}
   {:name :portal.command/toggle-expand
    ::shortcuts/default #{"e"}
    :run (fn->command state/toggle-expand)}
   {:name :portal.command/close-command-palette
    ::shortcuts/osx ["escape"]
    ::shortcuts/default ["escape"]
    :run close}
   {:name :portal.command/redo-previous-command
    ::shortcuts/default #{"control" "r"}
    :run (fn [state]
           (a/let [commands (::state/previous-commands @state)]
             (when (seq commands)
               (open
                (fn [_state]
                  [palette-component
                   {:on-select
                    (fn [{::keys [command]}]
                      (a/let [k (:portal/key command)
                              f (or (:portal/f command)
                                    (if (keyword? k)
                                      k
                                      (partial state/invoke (:portal/key command))))
                              args (:portal/args command)
                              value (apply f (state/get-selected-value @state) args)]
                        (state/dispatch!
                         state
                         state/history-push
                         (assoc command :portal/value value))))
                    :options
                    (for [command commands]
                      [palette-component-item
                       {::command command
                        ::value (:portal/key command)}
                       [s/div
                        {:style {:display :flex
                                 :justify-content :space-between
                                 :overflow :hidden
                                 :align-items :center
                                 :text-overflow :ellipsis
                                 :white-space :nowrap}}
                        [ins/inspector (:portal/key command)]
                        [s/div
                         {:style {:opacity 0.5}}
                         (for [a (:portal/args command)] (pr-str a))]]])}])))))}
   open-command-palette
   {:name :portal.command/theme-solarized-dark
    :run (fn [_state] (state/set-theme! ::c/solarized-dark))}
   {:name :portal.command/theme-solarized-light
    :run (fn [_state] (state/set-theme! ::c/solarized-light))}
   {:name :portal.command/theme-nord
    :run (fn [_state] (state/set-theme! ::c/nord))}
   {:name :portal.command/copy-as-edn
    ::shortcuts/osx #{"meta" "c"}
    ::shortcuts/default #{"control" "c"}
    :run
    (fn [state] (copy-edn! (state/get-selected-value @state)))}
   {:name :portal.command/copy-path
    :run (fn [state]
           (when-let [path (state/get-path @state)]
             (copy-edn! path)))}
   {:name :portal.command/history-back
    ::shortcuts/osx #{"meta" "arrowleft"}
    ::shortcuts/default #{"control" "arrowleft"}
    :run (fn [state] (state/dispatch! state state/history-back))}
   {:name :portal.command/history-forward
    ::shortcuts/osx #{"meta" "arrowright"}
    ::shortcuts/default #{"control" "arrowright"}
    :run (fn [state] (state/dispatch! state state/history-forward))}
   {:name :portal.command/history-first
    ::shortcuts/osx #{"meta" "shift" "arrowleft"}
    ::shortcuts/default #{"control" "shift" "arrowleft"}
    :run (fn [state] (state/dispatch! state state/history-first))}
   {:name :portal.command/history-last
    ::shortcuts/osx #{"meta" "shift" "arrowright"}
    ::shortcuts/default #{"control" "shift" "arrowright"}
    :run (fn [state] (state/dispatch! state state/history-last))}
   {:name :portal.command/clear
    ::shortcuts/default #{"control" "l"}
    :run (fn [state] (state/dispatch! state state/clear))}])

(def commands
  (concat
   (map
    make-command
    (concat clojure-commands
            portal-data-commands))
   portal-commands))

(defn pop-up [child]
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
     :overflow :hidden}}
   child])

(defn palette [props _value]
  (let [state (state/use-state)]
    (use-shortcuts
     ::commands
     (fn [log]
       (when-not (shortcuts/input? log)
         (doseq [command (concat commands (:commands props))]
           (when (shortcuts/match? command log)
             (shortcuts/matched! log)
             ((:run command) state))))))
    (when-let [component @input]
      [ins/with-readonly [pop-up [component state]]])))

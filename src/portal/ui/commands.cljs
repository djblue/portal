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

(defonce ^:private handlers (atom {}))
(def ^:private shortcut-context (react/createContext 0))

(defn- dispatch [log]
  (when-let [[_ f] (last (sort-by first < @handlers))]
    (f log)))

(defn- with-shortcuts [f & children]
  (let [i (react/useContext shortcut-context)]
    (react/useEffect
     (fn []
       (swap! handlers assoc i f)
       (fn []
         (swap! handlers dissoc i)))
     #js [f])
    (react/useEffect
     (fn []
       (shortcuts/add! ::with-shortcuts dispatch)
       (fn []
         (shortcuts/remove! ::with-shortcuts))))
    (into [:r> (.-Provider shortcut-context) #js {:value (inc i)}] children)))

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
        (fn [] [:div {:ref #(reset! el %)}])}))))

(defn selector-component []
  (let [selected (r/atom #{})
        active   (r/atom 0)]
    (fn [input]
      (let [theme     (theme/use-theme)
            selected? @selected
            on-close  close
            on-done   (:run input)
            options   (:options input)
            n         (count (:options input))
            on-select #(swap! selected (if (selected? %) disj conj) %)
            on-toggle (fn toggle-all []
                        (if (= (count @selected) (count options))
                          (reset! selected #{})
                          (swap! selected into options)))
            on-invert (fn invert []
                        (swap! selected #(set/difference (into #{} options) %)))
            style     {:font   :bold
                       :cursor :pointer
                       :color  (::c/string theme)}]
        [with-shortcuts
         (fn [log]
           (when
            (condp shortcuts/match? log
              "arrowup"        (swap! active #(mod (dec %) n))
              #{"shift" "tab"} (swap! active #(mod (dec %) n))
              "arrowdown"      (swap! active #(mod (inc %) n))
              "tab"            (swap! active #(mod (inc %) n))
              "a"       (on-toggle)
              "i"       (on-invert)
              " "       (on-select (nth options @active))
              "enter"   (on-done @selected)
              "escape"  (on-close)

              nil)
             (shortcuts/matched! log)))

         [container
          [s/div
           {:style {:box-sizing :border-box
                    :padding (:spacing/padding theme)
                    :user-select :none
                    :border-bottom [1 :solid (::c/border theme)]}}
           "Press "
           [:span
            {:style    style
             :on-click #(on-select (nth options @active))}
            "space"]
           " to select, "
           [:span
            {:style    style
             :on-click on-toggle}
            "a"]
           " to toggle all, "
           [:span
            {:style    style
             :on-click on-invert}
            "i"]
           " to invert and "
           [:span
            {:style    style
             :on-click #(on-done @selected)}
            "enter"]
           " to accept"]
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
                         :box-sizing     :border-box
                         :padding-left   (:spacing/padding theme)
                         :padding-top    (* 0.5 (:spacing/padding theme))
                         :padding-bottom (* 0.5 (:spacing/padding theme))
                         :cursor :pointer
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
                      [s/div {:style {:width (:spacing/padding theme)}}]
                      [ins/inspector option]])))
                doall)]]]))))

(def shortcut->symbol
  {"arrowright" "⭢"
   "arrowleft" "⭠"
   "arrowup" "⭡"
   "arrowdown" "⭣"
   "meta" "⌘"})

(defn combo-order [k]
  (get {"control" 0 "meta" 1 "shift" 2 "alt" 3} k 4))

(def ^:private keymap
  {{::shortcuts/osx     #{"meta" "shift" "p"}
    ::shortcuts/default #{"control" "shift" "p"}}
   'portal.command/open-command-palette

   {::shortcuts/default #{"control" "j"}}
   'portal.command/open-command-palette

   {::shortcuts/osx     #{"meta" "c"}
    ::shortcuts/default #{"control" "c"}}
   'portal.command/copy-as-edn

   {::shortcuts/osx     #{"meta" "arrowleft"}
    ::shortcuts/default #{"control" "arrowleft"}}
   'portal.command/history-back

   {::shortcuts/osx     #{"meta" "arrowright"}
    ::shortcuts/default #{"control" "arrowright"}}
   'portal.command/history-forward

   {::shortcuts/osx     #{"meta" "shift" "arrowleft"}
    ::shortcuts/default #{"control" "shift" "arrowleft"}}
   'portal.command/history-first

   {::shortcuts/osx     #{"meta" "shift" "arrowright"}
    ::shortcuts/default #{"control" "shift" "arrowright"}}
   'portal.command/history-last

   {::shortcuts/default #{"v"}}                 'portal.command/select-viewer
   {::shortcuts/default #{"arrowup"}}           'portal.command/select-prev
   {::shortcuts/default #{"arrowdown"}}         'portal.command/select-next
   {::shortcuts/default #{"arrowleft"}}         'portal.command/select-parent
   {::shortcuts/default #{"arrowright"}}        'portal.command/select-child

   {::shortcuts/default #{"control" "enter"}}   'portal.command/focus-selected
   {::shortcuts/default #{"e"}}                 'portal.command/toggle-expand

   {::shortcuts/default #{"enter"}}             'clojure.datafy/nav

   {::shortcuts/default #{"control" "r"}}       'portal.command/redo-previous-command
   {::shortcuts/default #{"control" "l"}}       'portal.command/clear

   ;; PWA
   {::shortcuts/osx     #{"meta" "o"}
    ::shortcuts/default #{"control" "o"}}       'portal.load/file
   {::shortcuts/osx     #{"meta" "v"}
    ::shortcuts/default #{"control" "v"}}       'portal.load/clipboard
   {::shortcuts/osx     #{"meta" "d"}
    ::shortcuts/default #{"control" "d"}}       'portal.load/demo})

(defn- find-combos [command]
  (let [command-name (:name command)]
    (some
     (fn [[combo f]]
       (when (= f command-name)
         (shortcuts/get-shortcut combo)))
     keymap)))

(defn shortcut [command]
  (let [theme (theme/use-theme)]
    (when-let [combo (find-combos command)]
      [s/div {:style
              {:display :flex
               :align-items :center
               :white-space :nowrap}}
       (for [k (sort-by combo-order combo)]
         ^{:key k}
         [s/div {:style
                 {:background "#0002"
                  :border-radius (:border-radius theme)
                  :box-sizing :border-box
                  :padding-top (* 0.25 (:spacing/padding theme))
                  :padding-bottom (* 0.25 (:spacing/padding theme))
                  :padding-left (:spacing/padding theme)
                  :padding-right (:spacing/padding theme)
                  :margin-right  (:spacing/padding theme)}}
          (get shortcut->symbol k (.toUpperCase k))])])))

(defn- palette-component-item [props & children]
  (let [theme (theme/use-theme)
        {:keys [active? on-click]} props]
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
         :height :fit-content
         :box-sizing     :border-box
         :padding-left   (:spacing/padding theme)
         :padding-top    (* 0.5 (:spacing/padding theme))
         :padding-bottom (* 0.5 (:spacing/padding theme))}
        (when active?
          {:border-left [5 :solid (::c/boolean theme)]
           :background (::c/background theme)}))
       :style/hover
       {:background (::c/background theme)}}]
     children)))

(defn palette-component []
  (let [active (r/atom 0)
        filter-text (r/atom "")]
    (fn [{:keys [on-select component filter-by options]
          :or   {component ins/inspector
                 filter-by identity}}]
      (let [theme (theme/use-theme)
            text @filter-text
            options
            (keep
             (fn [option]
               (when (or (str/blank? text)
                         (str/includes? (pr-str (filter-by option)) text))
                 option))
             options)
            n (count options)
            on-close close
            on-select
            (fn []
              (reset! filter-text "")
              (on-close)
              (when-let [option (nth options @active)]
                ;; Give react time to close command palette
                (js/setTimeout #(on-select option) 25)))]
        [with-shortcuts
         (fn [log]
           (when
            (condp shortcuts/match? log
              "arrowup"        (swap! active #(mod (dec %) n))
              #{"shift" "tab"} (swap! active #(mod (dec %) n))
              "arrowdown"      (swap! active #(mod (inc %) n))
              "tab"            (swap! active #(mod (inc %) n))
              "enter"          (on-select)
              "escape"         (on-close)
              nil)
             (shortcuts/matched! log)))
         [container
          [s/div
           {:style
            {:padding (:spacing/padding theme)
             :border-bottom [1 :solid (::c/border theme)]}}
           [s/input
            {:placeholder "Type to filter, <up> and <down> to move selection, <enter> to confirm."
             :auto-focus true
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
                      [palette-component-item
                       {:active? active?
                        :on-click on-click}
                       [component
                        option
                        {:active? active?
                         :on-click on-click}]]])))
                doall)]]]))))

(defn- empty-args [_] nil)

(defn fn->command [f]
  (fn [state]
    (when-let [selected (state/get-selected-context @state)]
      (state/dispatch! state f selected))))

(defn make-command [{:keys [name doc predicate args f]}]
  (let [predicate (or predicate (constantly true))]
    {:name name
     :doc doc
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
                  :portal/value result}))))}))

(defn get-functions [state]
  (a/let [v   (state/get-selected-value @state)
          fns (state/invoke 'portal.runtime/get-functions v)]
    (for [f fns]
      (make-command
       {:name f :f #(state/invoke f %)}))))

(declare commands)

(defn- command-item [command {:keys [active?]}]
  (let [theme (theme/use-theme)]
    [s/div
     {:style {:width "100%"}}
     [s/div
      {:style
       {:width "100%"
        :display :flex
        :align-items :center
        :justify-content :space-between}}
      [ins/inspector (:name command)]
      [shortcut command]]
     (when active?
       (when-let [doc (:doc command)]
         [s/div
          {:style
           {:box-sizing :border-box
            :padding (:spacing/padding theme)
            :background "rgba(0,0,0,0.20)"}}
          doc]))]))

(def open-command-palette
  {:name 'portal.command/open-command-palette
   :label "Show All Commands"
   :run (fn [state]
          (a/let [fns (get-functions state)
                  commands (remove
                            (fn [option]
                              (or
                               (#{'portal.command/open-command-palette}
                                (:name option))
                               (when-let [predicate (:predicate option)]
                                 (not (predicate @state)))))
                            (concat fns commands (:commands @state)))]
            (open
             (fn [state]
               [palette-component
                {:filter-by :name
                 :options commands
                 :component command-item
                 :on-select
                 (fn [command]
                   ((:run command) state))}]))))})

;; pick args

(defn pick-one [options]
  (js/Promise.
   (fn [resolve]
     (open
      (fn [_state]
        [palette-component
         {:on-select #(resolve [%])
          :options options}])))))

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
                 {:options (concat [::done] (keys v))
                  :on-select
                  (fn [k]
                    (let [path (conj path k)
                          next-value (get v k)]
                      (cond
                        (= k ::done)
                        (resolve [(drop-last path)])

                        (not (map? next-value))
                        (resolve [path])

                        :else
                        (get-key path next-value))))}])))]
       (get-key [] v)))))

;; clojure commands

(def clojure-commands
  [{:f vals
    :predicate map?
    :name 'clojure.core/vals
    :doc (:doc (meta #'clojure.core/vals))}
   {:f keys
    :predicate map?
    :name 'clojure.core/keys
    :doc (:doc (meta #'clojure.core/keys))}
   {:f count
    :predicate #(or (coll? %) (string? %))
    :name 'clojure.core/count
    :doc (:doc (meta #'clojure.core/count))}
   {:f first
    :predicate coll?
    :name 'clojure.core/first
    :doc (:doc (meta #'clojure.core/first))}
   {:f rest
    :predicate coll?
    :name 'clojure.core/rest
    :doc (:doc (meta #'clojure.core/rest))}
   {:f get
    :predicate map?
    :args (comp pick-one keys)
    :name 'clojure.core/get
    :doc (:doc (meta #'clojure.core/get))}
   {:f get-in
    :predicate map?
    :args pick-in
    :name 'clojure.core/get-in
    :doc (:doc (meta #'clojure.core/get-in))}
   {:f select-keys
    :predicate map?
    :args (comp pick-many keys)
    :name 'clojure.core/select-keys
    :doc (:doc (meta #'clojure.core/select-keys))}
   {:f (partial apply dissoc)
    :predicate map?
    :args (comp pick-many keys)
    :name 'clojure.core/dissoc
    :doc (:doc (meta #'clojure.core/dissoc))}])

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

(defn select-viewer [state]
  (when-let [selected-context (state/get-selected-context @state)]
    (let [viewers (ins/get-compatible-viewers @ins/viewers (:value selected-context))]
      (when (> (count viewers) 1)
        (a/let [[selected-viewer] (pick-one (map :name viewers))]
          (ins/set-viewer! state selected-context selected-viewer))))))

(defn copy-path [state]
  (when-let [path (state/get-path @state)]
    (copy-edn! path)))

(def portal-commands
  [{:name 'clojure.datafy/nav
    :run (fn [state]
           (state/dispatch!
            state
            state/nav
            (state/get-selected-context @state)))}
   {:name 'portal.command/select-viewer
    :run  select-viewer}
   {:name 'portal.command/select-prev
    :run (fn->command state/select-prev)}
   {:name 'portal.command/select-next
    :run (fn->command state/select-next)}
   {:name 'portal.command/select-parent
    :run (fn->command state/select-parent)}
   {:name 'portal.command/select-child
    :run (fn->command state/select-child)}
   {:name 'portal.command/focus-selected
    :run (fn->command state/focus-selected)}
   {:name 'portal.command/toggle-expand
    :run (fn->command state/toggle-expand)}
   {:name 'portal.command/redo-previous-command
    :run (fn [state]
           (a/let [commands (::state/previous-commands @state)]
             (when (seq commands)
               (open
                (fn [_state]
                  [palette-component
                   {:options commands
                    :filter-by :portal/key
                    :on-select
                    (fn [command]
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
                    :component
                    (fn [command]
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
                        (for [a (:portal/args command)] (pr-str a))]])}])))))}
   open-command-palette
   {:name 'portal.command/set-theme
    :run (fn [state]
           (a/let [[theme] (pick-one [::c/nord
                                      ::c/solarized-dark
                                      ::c/solarized-light
                                      ::c/material-ui])]
             (state/dispatch! state state/set-theme! theme)))}
   {:name 'portal.command/copy-as-edn
    :run
    (fn [state] (copy-edn! (state/get-selected-value @state)))}
   {:name 'portal.command/copy-path
    :run copy-path}
   {:name 'portal.command/history-back
    :run (fn [state] (state/dispatch! state state/history-back))}
   {:name 'portal.command/history-forward
    :run (fn [state] (state/dispatch! state state/history-forward))}
   {:name 'portal.command/history-first
    :run (fn [state] (state/dispatch! state state/history-first))}
   {:name 'portal.command/history-last
    :run (fn [state] (state/dispatch! state state/history-last))}
   {:name 'portal.command/clear
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
    [with-shortcuts
     (fn [log]
       (when-not (shortcuts/input? log)
         (first
          (for [[shortcut f] keymap
                command      (concat commands (:commands props))
                :when        (and (= (:name command) f)
                                  (shortcuts/match? shortcut log))]
            (do
              (shortcuts/matched! log)
              ((:run command) state))))))
     (when-let [component @input]
       [ins/with-readonly [pop-up [component state]]])]))

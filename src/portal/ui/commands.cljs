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
                    :padding (:padding theme)
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
                         :padding-left   (:padding theme)
                         :padding-top    (* 0.5 (:padding theme))
                         :padding-bottom (* 0.5 (:padding theme))
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
                      [s/div {:style {:width (:padding theme)}}]
                      [ins/inspector option]])))
                doall)]]]))))

(def ^:private keymap
  {{::shortcuts/osx     #{"meta" "shift" "p"}
    ::shortcuts/default #{"control" "shift" "p"}}
   `open-command-palette

   {::shortcuts/default #{"control" "j"}}
   `open-command-palette

   {::shortcuts/osx     #{"meta" "c"}
    ::shortcuts/default #{"control" "c"}}
   `copy

   {::shortcuts/osx     #{"meta" "arrowleft"}
    ::shortcuts/default #{"control" "arrowleft"}}
   `history-back

   {::shortcuts/osx     #{"meta" "arrowright"}
    ::shortcuts/default #{"control" "arrowright"}}
   `history-forward

   {::shortcuts/osx     #{"meta" "shift" "arrowleft"}
    ::shortcuts/default #{"control" "shift" "arrowleft"}}
   `history-first

   {::shortcuts/osx     #{"meta" "shift" "arrowright"}
    ::shortcuts/default #{"control" "shift" "arrowright"}}
   `history-last

   {::shortcuts/default ["/"]}                  `focus-filter

   {::shortcuts/default #{"v"}}                 `select-viewer
   {::shortcuts/default #{"arrowup"}}           `select-prev
   {::shortcuts/default #{"k"}}                 `select-prev
   {::shortcuts/default #{"arrowdown"}}         `select-next
   {::shortcuts/default #{"j"}}                 `select-next
   {::shortcuts/default #{"arrowleft"}}         `select-parent
   {::shortcuts/default #{"h"}}                 `select-parent
   {::shortcuts/default #{"arrowright"}}        `select-child
   {::shortcuts/default #{"l"}}                 `select-child

   {::shortcuts/default #{"control" "enter"}}   `focus-selected
   {::shortcuts/default #{"e"}}                 `toggle-expand

   {::shortcuts/default #{"enter"}}             'clojure.datafy/nav

   {::shortcuts/default #{"control" "r"}}       `redo-previous-command
   {::shortcuts/default #{"control" "l"}}       `clear

   {::shortcuts/default ["g" "g"]}              `scroll-top
   {::shortcuts/default #{"shift" "g"}}         `scroll-bottom

   ;; TODO Move to metadata of var when possible
   {::shortcuts/default ["g" "d"]}              `portal.runtime.jvm.editor/goto-definition

   ;; PWA
   {::shortcuts/osx     #{"meta" "o"}
    ::shortcuts/default #{"control" "o"}}       `portal.ui.pwa/open-file
   {::shortcuts/osx     #{"meta" "v"}
    ::shortcuts/default #{"control" "v"}}       `portal.ui.pwa/load-clipboard
   {::shortcuts/osx     #{"meta" "d"}
    ::shortcuts/default #{"control" "d"}}       `portal.ui.pwa/open-demo})

(def aliases {"cljs.core" "clojure.core"})

(defn- var->name [var]
  (let [{:keys [name ns]} (meta var)
        ns                (str ns)]
    (symbol (aliases ns ns) (str name))))

(defn- find-combos [command]
  (let [command-name (or (:name command)
                         (var->name command))]
    (sort-by
     #(into [] (sort %))
     (keep
      (fn [[combo f]]
        (when (= f command-name)
          (shortcuts/get-shortcut combo)))
      keymap))))

(def shortcut->symbol
  {"arrowright" "⭢"
   "arrowleft" "⭠"
   "arrowup" "⭡"
   "arrowdown" "⭣"
   "meta" "⌘"})

(defn combo-order [k]
  (get {"control" 0 "meta" 1 "shift" 2 "alt" 3} k 4))

(defn separate [coll]
  (let [theme (theme/use-theme)]
    [:<>
     (drop-last
      (interleave
       coll
       (for [i (-> coll count range)]
         ^{:key i}
         [s/div
          {:style {:box-sizing :border-box
                   :padding-left (:padding theme)
                   :padding-right (:padding theme)}}
          [s/div {:style {:height "100%"
                          :border-right [1 :solid (::c/border theme)]}}]])))]))

(defn shortcut [command]
  (let [theme (theme/use-theme)]
    [s/div {:style
            {:display :flex
             :align-items :stretch
             :white-space :nowrap}}
     [separate
      (for [combo (find-combos command)]
        [:<>
         {:key (hash combo)}
         (map-indexed
          (fn [index k]
            [s/div
             {:key index
              :style
              {:display :flex
               :align-items :center
               :background "#0002"
               :border-radius (:border-radius theme)
               :box-sizing :border-box
               :padding-top (* 0.25 (:padding theme))
               :padding-bottom (* 0.25 (:padding theme))
               :padding-left (:padding theme)
               :padding-right (:padding theme)
               :margin-right  (* 0.5 (:padding theme))
               :margin-left  (* 0.5 (:padding theme))}}
             (get shortcut->symbol k (.toUpperCase k))])
          (sort-by combo-order combo))])]]))

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
         :padding-left   (:padding theme)
         :padding-top    (* 0.5 (:padding theme))
         :padding-bottom (* 0.5 (:padding theme))}
        (when active?
          {:border-left [5 :solid (::c/boolean theme)]
           :background (::c/background theme)}))
       :style/hover
       {:background (::c/background theme)}}]
     children)))

(defn- stringify [props option]
  (pr-str
   (if-let [filter-by (:filter-by props)]
     (filter-by option)
     option)))

(defn- filter-options [props text]
  (if (str/blank? text)
    (:options props)
    (let [words (str/split text #"\s+")]
      (for [option  (:options props)
            :let    [s (stringify props option)]
            :when   (every? #(str/includes? s %) words)]
        option))))

(defn palette-component []
  (let [active (r/atom 0)
        filter-text (r/atom "")]
    (fn [{:keys [on-select component]
          :or   {component ins/inspector}
          :as options}]
      (let [theme (theme/use-theme)
            options (filter-options options @filter-text)
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
            {:padding (:padding theme)
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
              :padding (:padding theme)
              :box-sizing :border-box
              :font-size (:font-size theme)
              :font-family (:font-family theme)
              :color (::c/text theme)
              :border [1 :solid (::c/border theme)]}
             :style/placeholder {:color (::c/border theme)}}]]
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

(defn make-command [{:keys [name command predicate args f] :as opts}]
  (let [predicate (or predicate (constantly true))]
    (assoc opts
           :predicate (comp predicate state/get-selected-value)
           :run (fn [state]
                  (a/let [v      (state/get-selected-value @state)
                          args   ((or args empty-args) v)
                          result (apply f v args)]
                    (when-not command
                      (state/dispatch!
                       state
                       state/history-push
                       {:portal/key name
                        :portal/f f
                        :portal/args args
                        :portal/value result})))))))

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
            :padding (:padding theme)
            :background "rgba(0,0,0,0.20)"}}
          doc]))]))

(def registry (atom {}))
(def runtime-registry (atom nil))

(defn- get-commands []
  (concat @runtime-registry (vals @registry)))

(defn ^:command open-command-palette
  "Show All Commands"
  [state]
  (a/let [commands (sort-by
                    :name
                    (remove
                     (fn [option]
                       (or
                        (#{`open-command-palette}
                         (:name option))
                        (when-let [predicate (:predicate option)]
                          (not (predicate @state)))))
                     (get-commands)))]
    (open
     (fn [state]
       [palette-component
        {:filter-by :name
         :options commands
         :component command-item
         :on-select
         (fn [command]
           ((:run command) state))}]))))

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

(defn columns [value]
  (cond
    (map? value) (map-keys value)
    :else        (coll-keys value)))

(defn transpose-map
  "Transpose a map."
  [value]
  (reduce
   (fn [m path]
     (assoc-in m (reverse path) (get-in value path)))
   {}
   (for [row (keys value)
         column (map-keys value)
         :when (contains? (get value row) column)]
     [row column])))

(defn select-columns
  "Select column from list-of-maps or map-of-maps."
  [value ks]
  (cond
    (map? value)
    (reduce-kv
     (fn [v k m]
       (assoc v k (select-keys m ks)))
     value
     value)
    :else (map #(select-keys % ks) value)))

(defn- copy-to-clipboard! [s]
  (let [el (js/document.createElement "textarea")]
    (set! (.-value el) s)
    (js/document.body.appendChild el)
    (.select el)
    (js/document.execCommand "copy")
    (js/document.body.removeChild el)))

(defn- copy-edn! [value]
  (copy-to-clipboard!
   (str/trim
    (with-out-str
      (binding [*print-meta* true
                *print-length* 1000
                *print-level* 100]
        (pp/pprint value))))))

(defn ^:command copy
  "Copy selected value as an edn string to the clipboard."
  [state]
  (if-let [selection (not-empty (.. js/window getSelection toString))]
    (copy-to-clipboard! selection)
    (copy-edn! (state/get-selected-value @state))))

(defn ^:command select-viewer
  "Set the viewer for the currently selected value."
  [state]
  (when-let [selected-context (state/get-selected-context @state)]
    (let [viewers (ins/get-compatible-viewers @ins/viewers (:value selected-context))]
      (when (> (count viewers) 1)
        (a/let [[selected-viewer] (pick-one (map :name viewers))]
          (ins/set-viewer! state selected-context selected-viewer))))))

(defn ^:command copy-path
  "Copy the path from the root value to the currently selected item."
  [state]
  (when-let [path (state/get-path @state)]
    (copy-edn! path)))

(def filter-input (react/createRef))

(defn ^:command focus-filter [_]
  (when-let [input (.-current filter-input)]
    (.focus input)))

(def scroll-div (react/createRef))

(defn ^:command scroll-top [_]
  (when-let [div (.-current scroll-div)]
    (.scroll div #js {:top 0})))

(defn ^:command scroll-bottom [_]
  (when-let [div (.-current scroll-div)]
    (.scroll div #js {:top (+ (.-scrollHeight div) 1000)})))

(defn- apply-selected [state f]
  (when-let [selected (state/get-selected-context @state)]
    (state/dispatch! state f selected)))

(defn ^:command select-prev [state]
  (apply-selected state state/select-prev))

(defn ^:command select-next [state]
  (apply-selected state state/select-next))

(defn ^:command select-parent [state]
  (apply-selected state state/select-parent))

(defn ^:command select-child [state]
  (apply-selected state state/select-child))

(defn ^:command focus-selected [state]
  (apply-selected state state/focus-selected))

(defn ^:command toggle-expand [state]
  (apply-selected state state/toggle-expand))

(defn ^:command redo-previous-command [state]
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
               (for [a (:portal/args command)] (pr-str a))]])}])))))

(defn ^:command set-theme [state]
  (a/let [[theme] (pick-one (keys c/themes))]
    (state/dispatch! state state/set-theme! theme)))

(defn ^:command history-back [state]
  (state/dispatch! state state/history-back))
(defn ^:command history-forward [state]
  (state/dispatch! state state/history-forward))
(defn ^:command history-first [state]
  (state/dispatch! state state/history-first))
(defn ^:command history-last [state]
  (state/dispatch! state state/history-last))
(defn ^:command clear [state]
  (state/dispatch! state state/clear))

(defn ^:command show-client-errors [state]
  (state/dispatch! state state/history-push {:portal/value @state/errors}))

(defn- then-first [value] (.then value first))

(def clojure-commands
  {#'clojure.core/vals        {:predicate map?}
   #'clojure.core/keys        {:predicate map?}
   #'clojure.core/count       {:predicate #(or (coll? %) (string? %))}
   #'clojure.core/first       {:predicate coll?}
   #'clojure.core/rest        {:predicate coll?}
   #'clojure.core/get         {:predicate map? :args (comp pick-one keys)}
   #'clojure.core/get-in      {:predicate map? :args pick-in}
   #'clojure.core/select-keys {:predicate map? :args (comp pick-many keys)}
   #'clojure.core/dissoc      {:predicate map? :args (comp then-first pick-many keys)}})

(def portal-data-commands
  {#'transpose-map  {:predicate map-of-maps
                     :name      'portal.data/transpose-map}
   #'select-columns {:predicate (some-fn coll-of-maps map-of-maps)
                     :args      (comp pick-many columns)
                     :name      'portal.data/select-columns}})

(defn register!
  ([var] (register! var {}))
  ([var opts]
   (let [name (var->name var)]
     (swap! registry
            assoc name (merge {:name name
                               :run  var
                               :doc  (:doc (meta var))}
                              opts)))))

(doseq [var (vals (ns-publics 'portal.ui.commands))
        :when (-> var meta :command)]
  (register! var))

(doseq [[var opts] (merge clojure-commands portal-data-commands)]
  (let [name (var->name var)]
    (swap! registry
           assoc name (make-command (merge (meta var) {:f var :name name} opts)))))

(defn- nav [state]
  (state/dispatch!
   state
   state/nav
   (state/get-selected-context @state)))

(register! #'nav {:name      'clojure.datafy/nav
                  :predicate (comp :collection state/get-selected-context)})

(defn- get-style []
  (some-> js/document
          (.getElementsByTagName "html")
          (aget 0)
          (.getAttribute "style")
          not-empty))

(defn- get-vs-code-css-vars []
  (when-let [style (get-style)]
    (persistent!
     (reduce
      (fn [vars rule]
        (if-let [[attr value] (str/split rule #"\s*:\s*")]
          (assoc! vars attr value)
          vars))
      (transient {})
      (str/split style #"\s*;\s*")))))

(defn vs-code-vars
  "List all available css variable provided by vs-code."
  [state]
  (state/dispatch!
   state
   state/history-push
   {:portal/value (get-vs-code-css-vars)}))

(register! #'vs-code-vars {:predicate theme/is-vs-code?})

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

(defn palette []
  (let [state (state/use-state)
        value (state/get-selected-value @state)]
    (react/useEffect
     (fn []
       (a/let [fns (state/invoke 'portal.runtime/get-functions value)]
         (reset!
          runtime-registry
          (for [{:keys [name] :as opts} fns]
            (make-command
             (assoc opts :f #(state/invoke name %)))))))
     #js [(hash value)])
    [with-shortcuts
     (fn [log]
       (when-not (shortcuts/input? log)
         (first
          (for [[shortcut f] keymap
                command      (get-commands)
                :when        (and (= (:name command) f)
                                  (shortcuts/match? shortcut log))]
            (do
              (shortcuts/matched! log)
              ((:run command) state))))))
     (when-let [component @input]
       [ins/with-readonly [pop-up [component state]]])]))

(ns portal.ui.commands
  (:require [clojure.pprint :as pp]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [portal.async :as a]
            [portal.colors :as c]
            [portal.shortcuts :as shortcuts]
            [portal.ui.drag-and-drop :as dnd]
            [portal.ui.icons :as icons]
            [portal.ui.inspector :as ins]
            [portal.ui.options :as options]
            [portal.ui.parsers :as p]
            [portal.ui.react :as react]
            [portal.ui.state :as state]
            [portal.ui.styled :as s]
            [portal.ui.theme :as theme]
            [reagent.core :as r]))

(def ^:dynamic *state* nil)
(defonce ^:private input (r/atom nil))

(defn open
  ([f] (open input f))
  ([state f] (swap! state assoc ::input f)))

(defn close
  ([] (close input))
  ([state] (swap! state dissoc ::input)))

(defn- palette-container [& children]
  (let [theme (theme/use-theme)]
    (into
     [s/div
      {:on-click #(.stopPropagation %)
       :style
       {:font-family (:font-family theme)
        :max-height "100%"
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
(def ^:private shortcut-context (react/create-context 0))

(defn- dispatch [log]
  (when-let [[_ f] (last (sort-by first < @handlers))]
    (f log)))

(defn- with-shortcuts [f & children]
  (let [i (react/use-context shortcut-context)]
    (react/use-effect
     #js [f]
     (swap! handlers assoc i f)
     (fn []
       (swap! handlers dissoc i)))
    (react/use-effect
     :always
     (shortcuts/add! ::with-shortcuts dispatch)
     (fn []
       (shortcuts/remove! ::with-shortcuts)))
    (into [:r> (.-Provider shortcut-context) #js {:value (inc i)}] children)))

(defn- checkbox [checked?]
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

(defn- try-sort
  "Attempts to sort the given selection"
  [coll]
  (try
    (sort coll)
    (catch js/Error _
      coll)))

(defn- selector-component []
  (let [selected (r/atom #{})
        active   (r/atom 0)]
    (fn [input]
      (let [theme     (theme/use-theme)
            selected? @selected
            on-close  (partial close (state/use-state))
            on-done   (:run input)
            options   (try-sort (:options input))
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
              "k"              (swap! active #(mod (dec %) n))
              #{"shift" "tab"} (swap! active #(mod (dec %) n))
              "arrowdown"      (swap! active #(mod (inc %) n))
              "j"              (swap! active #(mod (inc %) n))
              "tab"            (swap! active #(mod (inc %) n))
              "a"       (on-toggle)
              "i"       (on-invert)
              " "       (on-select (nth options @active))
              "enter"   (on-done @selected)
              "escape"  (on-close)

              nil)
             (shortcuts/matched! log)))

         [palette-container
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

(def ^:private client-keymap (r/atom {}))

(def ^:private aliases {"cljs.core" "clojure.core"})

(defn- var->name [var]
  (let [{:keys [name ns]} (meta var)
        ns                (str ns)]
    (symbol (aliases ns ns) (str name))))

(defn- find-combos [keymap command]
  (let [command-name (:name command)]
    (keep
     (fn [[combo f]]
       (when (and (= f command-name)
                  (shortcuts/platform-supported? combo))
         combo))
     keymap)))

(def ^:private shortcut->symbol
  {"arrowright" [icons/arrow-right {:size "xs"}]
   "arrowleft"  [icons/arrow-left {:size "xs"}]
   "arrowup"    [icons/arrow-up {:size "xs"}]
   "arrowdown"  [icons/arrow-down {:size "xs"}]
   "meta"       "⌘"
   " "          "SPACE"
   "control"    "CTRL"})

(defn ^:private combo-order [k]
  (get {"control" 0 "meta" 1 "shift" 2 "alt" 3} k 4))

(defn- separate [coll]
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
  (let [theme (theme/use-theme)
        opts  (options/use-options)]
    [s/div {:style
            {:display :flex
             :align-items :stretch
             :white-space :nowrap}}
     [separate
      (for [combo (concat (find-combos @client-keymap command)
                          (find-combos (some-> opts :keymap deref) command))]
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

(defn- palette-component []
  (let [active (r/atom 0)
        filter-text (r/atom "")]
    (fn [{:keys [on-select component]
          :or   {component ins/inspector}
          :as options}]
      (let [theme (theme/use-theme)
            options (try-sort (filter-options options @filter-text))
            n (count options)
            on-close (partial close (state/use-state))
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
         [palette-container
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
                        {:active? active?
                         :on-click on-click}
                        option]]])))
                doall)]]]))))

(defn- can-meta? [value] (implements? IWithMeta value))

(defn- with-meta* [obj m]
  (if-not (can-meta? obj)
    obj
    (vary-meta obj #(merge m %))))

(defn make-command [{:keys [name command predicate args f] :as opts}]
  (assoc opts
         :predicate (fn [state]
                      (if-not predicate
                        true
                        (apply predicate (state/selected-values state))))
         :run (fn [state]
                (a/let [selected (for [context (:selected @state)]
                                   (with-meta*
                                     (:value context)
                                     {:portal.viewer/default (:name (ins/get-viewer state context))}))
                        args     (when args (binding [*state* state] (apply args selected)))
                        result   (a/try (apply f (concat selected args))
                                        (catch :default e (ex-data e)))]
                  (when-not command
                    (state/dispatch!
                     state
                     state/history-push
                     {:portal/key   name
                      :portal/f     f
                      :portal/args  args
                      :portal/value result}))))))

(defn- command-item [{:keys [active?]} command]
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
            :background "rgba(0,0,0,0.20)"
            :color (::c/text theme)}}
          doc]))]))

(def ^:no-doc registry
  (atom ^{:portal.viewer/default :portal.viewer/table
          :portal.viewer/table {:columns [:doc :command]}} {}))

(def ^:no-doc runtime-registry (atom nil))

(defn- get-commands []
  (merge @runtime-registry @registry))

(defn ^:command open-command-palette
  "Show All Commands"
  {:shortcuts [#{"control" "j"}
               #{"shift" ":"}
               ^::shortcuts/windows
               ^::shortcuts/linux
               #{"control" "shift" "p"}
               ^::shortcuts/osx
               #{"meta" "shift" "p"}]}
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
                     (vals (get-commands))))]
    (open
     state
     (fn [state]
       [palette-component
        {:filter-by :name
         :options commands
         :component command-item
         :on-select
         (fn [command]
           ((:run command) state))}]))))

;; pick args

(defn pick-one
  ([options] (pick-one *state* options))
  ([state options]
   (js/Promise.
    (fn [resolve]
      (open
       state
       (fn [_state]
         [palette-component
          {:on-select #(resolve [%])
           :options options}]))))))

(defn pick-many
  ([options]
   (pick-many *state* options))
  ([state options]
   (js/Promise.
    (fn [resolve]
      (open
       state
       (fn []
         (let [state (state/use-state)]
           [selector-component
            {:options options
             :run
             (fn [options]
               (close state)
               (resolve [options]))}])))))))

(defn pick-in
  ([v]
   (pick-in *state* v))
  ([state v]
   (js/Promise.
    (fn [resolve]
      (let [get-key
            (fn get-key [path v]
              (open
               state
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
        (get-key [] v))))))

;; portal data commands

(defn- coll-of-maps [value]
  (and (not (map? value))
       (coll? value)
       (every? map? value)))

(defn- map-of-maps [value]
  (and (map? value) (every? map? (vals value))))

(defn- coll-keys [value]
  (into [] (set (mapcat keys value))))

(defn- map-keys [value]
  (coll-keys (vals value)))

(defn- columns [value]
  (cond
    (map? value) (map-keys value)
    :else        (coll-keys value)))

(defn transpose-map
  "Transpose a map."
  [value]
  (with-meta
    (reduce
     (fn [m path]
       (assoc-in m (reverse path) (get-in value path)))
     {}
     (for [row (keys value)
           column (map-keys value)
           :when (contains? (get value row) column)]
       [row column]))
    (meta value)))

(defn select-columns
  "Select column from list-of-maps or map-of-maps."
  [value ks]
  (with-meta
    (cond
      (map? value)
      (reduce-kv
       (fn [v k m]
         (assoc v k (select-keys m ks)))
       value
       value)
      :else (map #(select-keys % ks) value))
    (meta value)))

(defn pprint
  "Pretty print selected value to a string"
  [value]
  (with-out-str (pp/pprint value)))

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

(defn- selected-values [state-val]
  (let [values (state/selected-values state-val)]
    (if (= (count values) 1)
      (first values)
      values)))

(defn ^:command copy
  "Copy selected value as an edn string to the clipboard."
  {:shortcuts [["y"]
               ^::shortcuts/osx #{"meta" "c"}
               ^::shortcuts/windows ^::shortcuts/linux #{"control" "c"}]}
  [state]
  (if-let [selection (not-empty (.. js/window getSelection toString))]
    (copy-to-clipboard! selection)
    (copy-edn! (selected-values @state))))

(defn- pprint-json [v]
  (.stringify js/JSON v nil 2))

(defn- keyword-fn [k]
  (str (when-let [n (namespace k)] (str n "/")) (name k)))

(defn ^:command copy-json
  "Copy selected value as a json string to the clipboard."
  [state]
  (-> @state
      selected-values
      (clj->js :keyword-fn keyword-fn)
      pprint-json
      str/trim
      copy-to-clipboard!))

(defn ^:command select-none
  "Deselect all values."
  {:shortcuts [#{"shift" "escape"}]}
  [state]
  (state/dispatch! state dissoc :selected))

(defn ^:command select-pop
  {:shortcuts [#{"escape"}]}
  [state]
  (state/dispatch! state state/select-pop))

(defn ^:command select-viewer
  "Set the viewer for the currently selected value(s)."
  {:shortcuts [#{"v"}]}
  [state]
  (when-let [selected-context (state/get-all-selected-context @state)]
    (let [viewers (ins/get-compatible-viewers @ins/viewers selected-context)]
      (when (> (count viewers) 1)
        (a/let [[selected-viewer] (pick-one state (map :name viewers))]
          (ins/set-viewer! state selected-context selected-viewer))))))

(defn- get-viewer [state context direction]
  (let [viewers (map :name (ins/get-compatible-viewers @ins/viewers context))
        current (:name (ins/get-viewer state (first context)))]
    (when (> (count viewers) 1)
      (some
       (fn [[prev next]]
         (case direction
           :prev (when (= next current) prev)
           :next (when (= prev current) next)))
       (partition 2 1 (conj viewers (last viewers)))))))

(defn ^:command select-prev-viewer
  {:shortcuts [#{"shift" "k"} #{"shift" "arrowup"}]}
  [state]
  (when-let [selected-context (state/get-all-selected-context @state)]
    (when-let [prev-viewer (get-viewer state selected-context :prev)]
      (ins/set-viewer! state selected-context prev-viewer))))

(defn ^:command select-next-viewer
  {:shortcuts [#{"shift" "j"} #{"shift" "arrowdown"}]}
  [state]
  (when-let [selected-context (state/get-all-selected-context @state)]
    (when-let [next-viewer (get-viewer state selected-context :next)]
      (ins/set-viewer! state selected-context next-viewer))))

(defn ^:command copy-path
  "Copy the path from the root value to the currently selected item."
  [state]
  (when-let [path (state/get-path @state)]
    (copy-edn! path)))

(defonce search-refs (atom #{}))

(defn ^:command focus-filter
  {:shortcuts [["/"]]}
  [_]
  (doseq [ref @search-refs]
    (when-let [input (.-current ref)] (.focus input))))

(defn ^:command clear-filter
  [state]
  (state/dispatch! state dissoc :search-text))

(defn ^:command scroll-top
  {:shortcuts [["g" "g"]]}
  [state]
  (when-let [el (:scroll-element @state)]
    (.scroll el #js {:top 0})))

(defn ^:command scroll-bottom
  {:shortcuts [#{"shift" "g"}]}
  [state]
  (when-let [el (:scroll-element @state)]
    (.scroll el #js {:top (+ (.-scrollHeight el) 1000)})))

(defn ^:command toggle-shell
  "Toggle visibility of top / bottom helper UX. Allows for a value focused session."
  [state]
  (state/dispatch! state update :disable-shell? not))

(defn- apply-selected [state f]
  (if-let [selected (state/get-selected-context @state)]
    (state/dispatch! state f selected)
    (state/dispatch! state state/select-root)))

(defn ^:command select-root [state]
  (state/dispatch! state state/select-root))

(defn ^:command select-prev
  {:shortcuts [#{"arrowup"} #{"k"}]}
  [state]
  (apply-selected state state/select-prev))

(defn ^:command select-next
  {:shortcuts [#{"arrowdown"} #{"j"}]}
  [state]
  (apply-selected state state/select-next))

(defn ^:command select-parent
  {:shortcuts [#{"arrowleft"} #{"h"}]}
  [state]
  (apply-selected state state/select-parent))

(defn ^:command select-child
  {:shortcuts [#{"arrowright"} #{"l"}]}
  [state]
  (apply-selected state state/select-child))

(defn ^:command focus-selected
  {:shortcuts [#{"control" "enter"}]}
  [state]
  (apply-selected state state/focus-selected))

(defn ^:command toggle-expand
  "Expand or collapse currently selected values."
  {:shortcuts [["e"] [" "] ["z" "a"]]}
  [state]
  (state/dispatch! state state/toggle-expand))

(defn ^:command expand-inc
  "Expand 1 additional layer of children from selected values."
  {:shortcuts [#{"shift" "e"} #{"shift" " "}]}
  [state]
  (state/dispatch! state state/expand-inc))

(defn ^:command redo-previous-command
  {:shortcuts [#{"control" "r"}]}
  [state]
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
  (a/let [[theme] (pick-one state (keys c/themes))]
    (state/dispatch! state state/set-theme! theme)))

(defn ^:command history-back
  {:shortcuts
   [#{"shift" "h"}
    ^::shortcuts/osx #{"meta" "arrowleft"}
    ^::shortcuts/windows ^::shortcuts/linux #{"control" "arrowleft"}]}
  [state]
  (state/dispatch! state state/history-back))

(defn ^:command history-forward
  {:shortcuts
   [#{"shift" "l"}
    ^::shortcuts/osx #{"meta" "arrowright"}
    ^::shortcuts/windows ^::shortcuts/linux #{"control" "arrowright"}]}
  [state]
  (state/dispatch! state state/history-forward))

(defn ^:command history-first
  {:shortcuts
   [^::shortcuts/osx #{"meta" "shift" "arrowleft"}
    ^::shortcuts/windows ^::shortcuts/linux #{"control" "shift" "arrowleft"}]}
  [state]
  (state/dispatch! state state/history-first))

(defn ^:command history-last
  {:shortcuts
   [^::shortcuts/osx #{"meta" "shift" "arrowright"}
    ^::shortcuts/windows ^::shortcuts/linux #{"control" "shift" "arrowright"}]}
  [state]
  (state/dispatch! state state/history-last))

(defn ^:command clear
  {:shortcuts [#{"control" "l"}]}
  [state]
  (state/dispatch! state state/clear))

(defn ^:command show-rpc-log
  "Show up to the last 10 RPC the Portal UI has made."
  [state]
  (state/dispatch! state state/history-push {:portal/value @state/log}))

(defn- then-first [value] (.then value first))

(defn- when-one [f]
  (fn [& args]
    (when (= (count args) 1)
      (f (first args)))))

(def ^:private clojure-commands
  {#'clojure.core/vals        {:predicate map?}
   #'clojure.core/keys        {:predicate map?}
   #'clojure.core/count       {:predicate #(or (coll? %) (string? %))}
   #'clojure.core/first       {:predicate coll?}
   #'clojure.core/rest        {:predicate coll?}
   #'clojure.core/get         {:predicate map? :args (when-one (comp pick-one keys))}
   #'clojure.core/get-in      {:predicate map? :args (when-one pick-in)}
   #'clojure.core/select-keys {:predicate map? :args (when-one (comp pick-many keys))}
   #'walk/keywordize-keys     {}
   #'clojure.core/dissoc      {:predicate map? :args (when-one (comp then-first pick-many keys))}
   #'clojure.core/vector      {}
   #'clojure.core/str         {}
   #'clojure.core/concat      {:predicate (fn [& args] (every? coll? args))}
   #'clojure.core/contains?   {:predicate (fn [coll & args]
                                            (and (coll? coll) (= (count args) 1)))}
   #'clojure.core/merge       {:predicate (fn [& args] (every? map? args))}})

(def ^:private portal-data-commands
  {#'pprint         {:name      'portal.data/pprint
                     :predicate any?}
   #'transpose-map  {:predicate map-of-maps
                     :name      'portal.data/transpose-map}
   #'select-columns {:predicate (some-fn coll-of-maps map-of-maps)
                     :args      (comp pick-many columns)
                     :name      'portal.data/select-columns}})

(defn register!
  ([var] (register! var {}))
  ([var opts]
   (let [m    (meta var)
         name (or (:name opts) (var->name var))]
     (doseq [shortcut (concat (:shortcuts m) (:shortcuts opts))]
       (swap! client-keymap assoc shortcut name))
     (swap! registry
            assoc name (merge {:name name :run  var}
                              (when-let [doc (or (:doc m) (:doc opts))] {:doc doc})
                              (when-let [command (:command m)] {:command command})
                              opts)))))

(doseq [var (vals (ns-publics 'portal.ui.commands))
        :when (-> var meta :command)]
  (register! var))

(doseq [[var opts] (merge clojure-commands portal-data-commands)]
  (let [name (var->name var)]
    (register! var (make-command (merge (meta var) {:f var :name name} opts)))))

(register! #'state/close {:name 'portal.api/close})

(defn- nav
  "Returns (possibly transformed) v in the context of coll and k (a
  key/index or nil). Callers should attempt to provide the key/index
  context k for Indexed/Associative/ILookup colls if possible, but not
  to fabricate one e.g. for sequences (pass nil). nav will return the
  value of clojure.core.protocols/nav."
  [state]
  (state/dispatch!
   state
   state/nav
   (state/get-selected-context @state)))

(register! #'nav {:name      'clojure.datafy/nav
                  :predicate (comp :collection state/get-selected-context)})

(defn- vs-code-vars
  "List all available css variable provided by vs-code."
  [state]
  (state/dispatch!
   state
   state/history-push
   {:portal/value (theme/get-vs-code-css-vars)}))

(register! #'vs-code-vars {:predicate theme/is-vs-code?})

(defn- vs-code-copy-theme
  [_]
  (-> (theme/get-vs-code-css-vars)
      (walk/postwalk-replace (::c/vs-code-embedded c/themes))
      (copy-edn!)))

(register! #'vs-code-copy-theme {:predicate theme/is-vs-code?})

(defn copy-str
  "Copy string to the clipboard."
  {:shortcuts [#{"shift" "c"}]}
  [state]
  (copy-to-clipboard! (state/get-selected-value @state)))

(register! #'copy-str {:predicate (comp string? state/get-selected-value)})

(defn- prompt-file []
  (js/Promise.
   (fn [resolve _reject]
     (let [id      "open-file-dialog"
           input   (or
                    (js/document.getElementById id)
                    (js/document.createElement "input"))]
       (set! (.-id input) id)
       (set! (.-type input) "file")
       (set! (.-multiple input) "true")
       (set! (.-style input) "visibility:hidden")
       (.addEventListener
        input
        "change"
        (fn [event]
          (a/let [value (dnd/handle-files (-> event .-target .-files))]
            (resolve value)))
        false)
       (js/document.body.appendChild input)
       (.click input)))))

(defn open-file
  "Open a File"
  {:shortcuts
   [^::shortcuts/osx #{"meta" "o"}
    ^::shortcuts/windows ^::shortcuts/linux #{"control" "o"}]}
  [state]
  (a/let [value (prompt-file)]
    (state/dispatch! state state/history-push {:portal/value value})))

(register! #'open-file)

(defn- clipboard []
  (js/navigator.clipboard.readText))

(defn- parse-as
  "Paste value from clipboard"
  [state value]
  (a/let [[format] (pick-one state (p/formats))]
    (state/dispatch! state
                     state/history-push
                     {:portal/value
                      (try (p/parse-string format value) (catch :default e e))})))

(defn paste
  "Paste value from clipboard"
  {:shortcuts
   [["p" "p"]
    ^::shortcuts/osx #{"meta" "v"}
    ^::shortcuts/windows ^::shortcuts/linux #{"control" "v"}]}
  [state]
  (a/let [value (clipboard)] (parse-as state value)))

(register! #'paste)

(defn parse-selected
  "Parse currently select text"
  {:shortcuts [["p" "s"]]}
  [state]
  (parse-as state (.toString (.getSelection js/window))))

(register! #'parse-selected)

(defn- pop-up [child]
  (let [state (state/use-state)]
    [s/div
     {:on-click (fn [_] (close state))
      :style
      {:position :fixed
       :top 0
       :left 0
       :right 0
       :bottom 0
       :z-index 100
       :padding "10%"
       :box-sizing :border-box
       :height "100%"
       :overflow :hidden}}
     child]))

(defn palette [{:keys [container]}]
  (let [state (state/use-state)
        value (state/get-selected-value @state)
        opts  (options/use-options)]
    (react/use-effect
     #js [(hash value)]
     (a/let [fns (state/invoke 'portal.runtime/get-functions value)]
       (reset!
        runtime-registry
        (update-vals
         fns
         (fn [opts]
           (make-command
            (assoc opts :f (partial state/invoke (:name opts)))))))))
    [with-shortcuts
     (fn [log]
       (when-not (shortcuts/input? log)
         (when-let [f (shortcuts/match (merge @client-keymap (some-> opts :keymap deref)) log)]
           (when-let [{:keys [run]} (or (get @registry f)
                                        (get @runtime-registry f))]
             (shortcuts/matched! log)
             (run state)))))
     (when-let [component (::input @state)]
       [ins/with-readonly [(or container pop-up) [component state]]])]))

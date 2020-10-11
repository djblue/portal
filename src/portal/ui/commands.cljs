(ns portal.ui.commands
  (:require [clojure.string :as str]
            [clojure.pprint :as pp]
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

(defonce open? (r/atom false))

(def commands
  [{:name :portal.command/command-palette
    ::shortcuts/osx #{"meta" "shift" "p"}
    ::shortcuts/default #{"control" "shift" "p"}
    :run (fn [] (reset! open? true))}
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

(defn register! [settings]
  (doseq [{:keys [run] :as command} commands]
    (shortcuts/register! command #(run settings))))

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
    (fn [settings]
      (let [commands
            (remove
             (fn [command]
               (let [text @filter-text]
                 (or
                  (= (:name command) :portal.command/command-palette)
                  (when-not (str/blank? text)
                    (not (str/includes? (pr-str (:name command)) text))))))
             commands)
            n (count commands)
            on-close #(reset! open? false)
            run (fn []
                  (reset! filter-text "")
                  (on-close)
                  (when-let [run (:run (nth commands @active))]
                    (run settings)))]
        [s/div
         {:on-click on-close
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
            :border-radius (:border-radius settings)}}
          [s/div
           {:style
            {:padding (:spacing/padding settings)
             :border-bottom (str "1px solid " (::c/border settings))}}
           [s/input
            {:auto-focus true
             :value @filter-text
             :on-change #(do
                           (reset! active 0)
                           (reset! filter-text (.-value (.-target %))))
             :on-key-down
             (fn [e]
               (let [k (.-key e)]
                 (cond
                   (= k "ArrowUp")
                   (swap! active #(mod (dec %) n))

                   (= k "ArrowDown")
                   (swap! active #(mod (inc %) n))

                   (= k "Escape")
                   (on-close)

                   (= k "Enter")
                   (run))))
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
           (->> commands
                (map-indexed
                 (fn [index command]
                   (let [active? (= index @active)]
                     ^{:key index}
                     [s/div
                      {:on-click #(do
                                    (reset! active index)
                                    (run))
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
                      [inspector settings (:name command)]
                      [shortcut settings command]])))
                doall)]]]))))

(defn palette [settings value]
  (register! settings)
  (when @open?
    [palette-component settings value]))

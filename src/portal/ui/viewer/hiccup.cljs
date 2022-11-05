(ns portal.ui.viewer.hiccup
  (:require [portal.colors :as c]
            [portal.ui.inspector :as ins]
            [portal.ui.select :as select]
            [portal.ui.styled :as s]
            [portal.ui.theme :as theme]
            [portal.ui.viewer.code :as code]))

(defn- header-styles [theme]
  {:color (::c/namespace theme)
   :margin 0
   :padding-top (:padding theme)
   :padding-bottom (:padding theme)
   :margin-bottom (* 2 (:padding theme))})

(defn- add-class [m]
  (reduce-kv
   (fn [out k v]
     (assoc out [:.hiccup k] v))
   {}
   m))

(defn styles []
  (let [theme (theme/use-theme)
        h     (header-styles theme)
        bg    (ins/get-background)
        bg2   (ins/get-background2)
        border-bottom
        {:border-bottom
         (str "1px solid " (::c/border theme))}]
    [:style
     (s/map->css
      (add-class
       {:h1 (merge {:font-size "2em"} h border-bottom)
        :h2 (merge {:font-size "1.5em"} h border-bottom)

        :h3 h :h4 h :h5 h :h6 h

        :a {:color (::c/uri theme)}

        :ul {:margin 0}
        :ol {:margin 0}
        :li {:margin-top (* 0.5 (:padding theme))}

        :p {:font-family "-apple-system,BlinkMacSystemFont,Segoe UI,Helvetica,Arial,sans-serif,Apple Color Emoji,Segoe UI Emoji"
            :font-size (:font-size theme)
            :line-height "1.5em"
            :margin 0}

        :img {:max-width "100%"}

        :code
        {:background (::c/background2 theme)
         :border-radius (:border-radius theme)
         :box-sizing :border-box
         :padding [0 (* 1.5 (:padding theme))]}

        :pre
        {:overflow :auto
         :background (::c/background2 theme)
         :border-radius (:border-radius theme)}

        :blockquote
        {:margin 0
         :background bg
         :padding-left (* 2 (:padding theme))
         :border-left [(:padding theme) :solid (::c/border theme)]}

        :table {:color (::c/text theme)
                :font-size (:font-size theme)
                :width "100%"
                :overflow :auto
                :border-style :hidden
                :border-collapse :collapse
                :border-radius (:border-radius theme)
                :box-shadow [0 0 0 1 (::c/border theme)]}
        :th {:font-weight :bold
             :text-align :center
             :padding (* 1.5 (:padding theme))
             :border [1 :solid (::c/border theme)]}
        :td {:text-align :left
             :padding (:padding theme)
             :border [1 :solid (::c/border theme)]}

        "tr:nth-child(odd)"  {:background bg}
        "tr:nth-child(even)" {:background bg2}

        "> div"
        {:display :flex
         :flex-direction :column
         :gap (* 1.5 (:padding theme))}}))]))

(defn inspect-code [& args]
  (let [[_ attrs code] (second args)
        theme          (theme/use-theme)]
    [s/div
     {:style
      {:margin-top    (:padding theme)
       :margin-bottom (:padding theme)}}
     [code/inspect-code attrs code]]))

(def tag->viewer
  {:pre inspect-code})

(defn- process-hiccup [context hiccup]
  (if-not (vector? hiccup)
    hiccup
    (let [[tag & args]  hiccup
          component     (or (get-in context [:viewers tag :component])
                            (tag->viewer tag))]
      (if component
        [ins/dec-depth
         [select/with-position
          {:row 0 :column 0}
          (into [component] args)]]
        (into
         [tag]
         (map #(process-hiccup context %))
         args)))))

(defn inspect-hiccup [value]
  (let [viewers (ins/viewers-by-name @ins/viewers)]
    [ins/with-key
     :portal.viewer/hiccup
     [:div {:class "hiccup"}
      (process-hiccup {:viewers viewers} value)]]))

(defn hiccup? [value]
  (and (vector? value)
       (keyword? (first value))))

(def viewer
  {:predicate hiccup?
   :component inspect-hiccup
   :name :portal.viewer/hiccup})

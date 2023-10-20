(ns ^:no-doc portal.ui.viewer.markdown
  (:require ["marked" :refer [marked]]
            [clojure.string :as str]
            [portal.colors :as c]
            [portal.ui.icons :as icons]
            [portal.ui.inspector :as ins]
            [portal.ui.lazy :as l]
            [portal.ui.styled :as d]
            [portal.ui.theme :as theme]
            [portal.ui.viewer.hiccup :refer [inspect-hiccup]]))

(declare ->inline)
(declare ->hiccup)

(def ^:dynamic *context* nil)

(defn- unescape [^string string]
  (-> string
      (.replaceAll "&amp;"  "&")
      (.replaceAll "&lt;"   "<")
      (.replaceAll "&gt;"   ">")
      (.replaceAll "&quot;" "\"")
      (.replaceAll "&#39;"  "'")))

(defn- ->text [^js token]
  (unescape (.-text token)))

(defn- ->style [string]
  (persistent!
   (reduce
    (fn [style rule]
      (let [[k v] (str/split rule #":")]
        (assoc! style (keyword (str/trim k)) (str/trim v))))
    (transient {})
    (str/split string #";"))))

(defn- dom->hiccup [^js el]
  (case (.-nodeType el)
    3 (.-wholeText el)
    1 (let [attrs (.-attributes el)]
        (into
         [(keyword (str/lower-case (.-tagName el)))
          (persistent!
           (reduce
            (fn [attrs ^js attr]
              (let [k (keyword (.-name attr))]
                (assoc! attrs k
                        (case k
                          :style (->style (.-value attr))
                          (.-value attr)))))
            (transient {})
            attrs))]
         (map dom->hiccup)
         (.-childNodes el)))))

(defn- parse-dom [string]
  (-> (js/DOMParser.)
      (.parseFromString string "text/html")
      (.getElementsByTagName "body")
      (aget 0)
      (.-childNodes)))

(defn- parse-html [html]
  (into [:<>] (map dom->hiccup) (parse-dom html)))

(defn- absolute-link? [href]
  (or (str/starts-with? href "http")
      (str/starts-with? href "https")))

(defn- ->link [^js token]
  (let [href (.-href token)]
    (->inline
     (let [title (.-title token)]
       [:a (cond-> {:href  href
                    :target "_blank"
                    :on-click
                    (fn [e]
                      (when-not (absolute-link? href)
                        (.preventDefault e)))}
             title (assoc :title title))])
     (.-tokens token))))

(defn- ->image [^js token]
  (let [title (.-title token) alt (->text token)]
    [:img (cond-> {:src (.-href token)}
            title (assoc :title title)
            alt (assoc :alt alt))]))

(def ^:private callouts
  {"Note"
   {:icon icons/info-circle
    :color ::c/boolean}
   "Warning"
   {:icon icons/exclamation-triangle
    :color ::c/tag}})

(defn- ->strong [^js token]
  (let [theme  (:theme *context*)
        tokens (->inline [] (.-tokens token))]
    (into
     (if-let [{:keys [icon color]}
              (and (= :blockquote (:parent *context*))
                   (get callouts (first tokens)))]
       [:strong
        {:style {:color (get theme color)}}
        [icon {:size "1x"}] " "]
       [:strong {}])
     tokens)))

(defn- ->header [^js token]
  [:thead
   {}
   (persistent!
    (reduce
     (fn [out ^js cell]
       (conj!
        out
        (->inline
         [:th {:align (.-align token)}]
         (.-tokens cell))))
     (transient [:tr {}])
     (.-header token)))])

(defn ->row [^js token ^js row]
  (persistent!
   (reduce
    (fn [out ^js cell]
      (conj!
       out
       (->inline
        [:td {:align (.-align token)}]
        (.-tokens cell))))
    (transient [:tr {}])
    row)))

(defn- ->rows [^js token]
  (persistent!
   (reduce
    (fn [out row]
      (conj! out (->row token row)))
    (transient [:tbody {}])
    (.-rows token))))

(defn- ->table [^js token]
  [:table (->header token) (->rows token)])

(defn- ->list [^js token]
  (let [ordered (.-ordered token)
        start   (.-start token)
        _loose   (.-loose token)
        tag     (if ordered :ol :ul)]
    (persistent!
     (reduce
      (fn [out ^js item]
        (let [_checked (.-checked item)
              _task    (.-task item)]
          (conj! out [:li {} (->hiccup (.-tokens item))])))
      (transient [tag {:start start}])
      (.-items token)))))

(defn- ->code [^js token]
  (let [lang (.-lang token)]
    [:pre {}
     [:code
      (cond-> {} (seq lang) (assoc :class lang))
      (->text token)]]))

(defn- ->heading [^js token]
  (let [tag (keyword (str "h" (.-depth token)))]
    (->inline [tag {}] (.-tokens token))))

(defn- ->paragraph [^js token]
  (->inline [:p {}] (.-tokens token)))

(defn- ->blockquote [^js token]
  (binding [*context* (assoc *context* :parent :blockquote)]
    (->hiccup [:blockquote {}] (.-tokens token))))

(defn- ->inline [out tokens]
  (reduce
   (fn [out ^js token]
     (case (.-type token)
       "escape"   (conj out (->text token))
       "html"     (conj out (parse-html (.-text token)))
       "link"     (conj out (->link token))
       "image"    (conj out (->image token))
       "strong"   (conj out (->strong token))
       "em"       (conj out (->inline [:em {}] (.-tokens token)))
       "codespan" (conj out [:code {} (->text token)])
       "br"       (conj out [:br {}])
       "del"      (conj out (->inline [:del {}] (.-tokens token)))
       "text"     (conj out (->text token))))
   out
   tokens))

(defn ->hiccup
  ([tokens]
   (->hiccup [:<>] tokens))
  ([out tokens]
   (reduce
    (fn [out ^js token]
      (case (.-type token)
        "space"      out
        "hr"         (conj out [:hr {}])
        "heading"    (conj out (->heading token))
        "code"       (conj out (->code token))
        "table"      (conj out (->table token))
        "blockquote" (conj out (->blockquote token))
        "list"       (conj out (->list token))
        "html"       (conj out (parse-html (->text token)))
        "paragraph"  (conj out (->paragraph token))
        "text"       (if-let [tokens (.-tokens token)]
                       (->inline out tokens)
                       (conj out (->text token)))))
    out
    tokens)))

(defn ^:no-doc parse-markdown [value]
  (->hiccup (.lexer marked value)))

(defn- inspect-markdown* [value]
  (binding [*context* {:theme (theme/use-theme)}]
    (let [opts (ins/use-options)]
      [inspect-hiccup
       [d/div
        {:style
         (merge
          {:gap 16
           :width 896
           :max-width "100%"
           :display :flex
           :box-sizing :border-box
           :flex-direction :column}
          (get-in opts [:props :style]))}
        (parse-markdown value)]])))

(defn inspect-markdown [value]
  [l/lazy-render [inspect-markdown* value]])

(def viewer
  {:predicate string?
   :component inspect-markdown
   :name :portal.viewer/markdown
   :doc "Parse string as markdown and view as html."})

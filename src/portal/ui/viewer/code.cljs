(ns ^:no-doc portal.ui.viewer.code
  (:require ["highlight.js" :as hljs]
            [clojure.string :as str]
            [portal.colors :as c]
            [portal.ui.filter :as f]
            [portal.ui.html :as h]
            [portal.ui.inspector :as ins]
            [portal.ui.styled :as s]
            [portal.ui.theme :as theme]))

(def ^:private root-class "portal-viewer-code")

(defn- ->styles [theme]
  (let [{::c/keys [uri keyword text namespace background string package number tag border]} theme]
    (update-keys
     {"pre code.hljs"                                 {:display :block :overflow-x :auto :padding "1em"}
      "code.hljs"                                     {:padding "3px 5px"}
      ".hljs"                                         {:background background}
      ".hljs,.hljs-subst"                             {:color text}
      ".hljs-selector-tag"                            {:color "#81a1c1"}
      ".hljs-selector-id"                             {:color namespace :font-weight 700}
      ".hljs-selector-attr,.hljs-selector-class"      {:color namespace}
      ".hljs-property,.hljs-selector-pseudo"          {:color package}
      ".hljs-addition"                                {:background-color "rgba(163,190,140,.5)"}
      ".hljs-deletion"                                {:background-color "rgba(191,97,106,.5)"}
      ".hljs-built_in,.hljs-class,.hljs-type"         {:color namespace}
      ".hljs-function"                                {:color package}
      ".hljs-function>.hljs-title"                    {:color package}
      ".hljs-title.hljs-function"                     {:color package}
      ".hljs-keyword"                                 {:color keyword}
      ".hljs-literal"                                 {:color text}
      ".hljs-symbol"                                  {:color keyword}
      ".hljs-number"                                  {:color number}
      ".hljs-regexp"                                  {:color tag}
      ".hljs-string"                                  {:color string}
      ".hljs-title"                                   {:color symbol}
      ".hljs-params"                                  {:color text}
      ".hljs-bullet"                                  {:color border}
      ".hljs-code"                                    {:color namespace}
      ".hljs-emphasis"                                {:font-style :italic}
      ".hljs-formula"                                 {:color namespace}
      ".hljs-strong"                                  {:font-weight 700}
      ".hljs-link:hover"                              {:text-decoration :underline}
      ".hljs-comment,.hljs-quote"                     {:color border}
      ".hljs-doctag"                                  {:color namespace}
      ".hljs-meta,.hljs-meta .hljs-keyword"           {:color keyword}
      ".hljs-meta .hljs-string"                       {:color string}
      ".hljs-attr"                                    {:color namespace}
      ".hljs-attribute"                               {:color text}
      ".hljs-name"                                    {:color namespace}
      ".hljs-section"                                 {:color package}
      ".hljs-tag"                                     {:color tag}
      ".hljs-template-variable,.hljs-variable"        {:color text}
      ".hljs-template-tag"                            {:color keyword}
      ".language-abnf .hljs-attribute"                {:color package}
      ".language-abnf .hljs-symbol"                   {:color tag}
      ".language-apache .hljs-attribute"              {:color package}
      ".language-apache .hljs-section"                {:color border}
      ".language-arduino .hljs-built_in"              {:color package}
      ".language-aspectj .hljs-meta"                  {:color uri}
      ".language-aspectj>.hljs-title"                 {:color package}
      ".language-bnf .hljs-attribute"                 {:color namespace}
      ".language-coq .hljs-built_in"                  {:color package}
      ".language-cpp .hljs-meta .hljs-string"         {:color string}
      ".language-css .hljs-built_in"                  {:color package}
      ".language-css .hljs-keyword"                   {:color uri}
      ".language-diff .hljs-meta"                     {:color namespace}
      ".language-ebnf .hljs-attribute"                {:color namespace}
      ".language-glsl .hljs-built_in"                 {:color package}
      ".language-groovy .hljs-meta:not(:first-child)" {:color uri}
      ".language-haxe .hljs-meta"                     {:color uri}
      ".language-java .hljs-meta"                     {:color uri}
      ".language-ldif .hljs-attribute"                {:color namespace}
      ".language-lisp .hljs-name"                     {:color package}
      ".language-lua .hljs-built_in"                  {:color package}
      ".language-moonscript .hljs-built_in"           {:color package}
      ".language-nginx .hljs-attribute"               {:color package}
      ".language-nginx .hljs-section"                 {:color keyword}
      ".language-pf .hljs-built_in"                   {:color package}
      ".language-processing .hljs-built_in"           {:color package}
      ".language-scss .hljs-keyword"                  {:color keyword}
      ".language-stylus .hljs-keyword"                {:color keyword}
      ".language-swift .hljs-meta"                    {:color uri}
      ".language-vim .hljs-built_in"                  {:color package :font-style :italic}
      ".language-yaml .hljs-meta"                     {:color uri}}
     (fn [selector]
       (str "." root-class " " selector)))))

(defn- ->css [styles]
  (str/join
   (for [[class style] styles]
     (str class "{" (s/style->css style) "}"))))

(def ^:private language-map {"emacs-lisp" "lisp" "elisp" "lisp"})

(defn ^:no-doc stylesheet []
  (let [theme    (theme/use-theme)]
    [:style
     (->css (->styles theme))]))

(defn highlight-clj [string-value]
  (let [theme (theme/use-theme)]
    [:pre
     {:class root-class
      :style {:margin      0
              :padding     0
              :background  :none
              :width       "100%"
              :white-space :pre-wrap
              :color       (::c/text theme)
              :font-size   (:font-size theme)
              :font-family (:font-family theme)}}
     [h/html+
      (-> string-value
          (hljs/highlight  #js {:language "clojure"})
          .-value)]]))

(defn inspect-pr-str [value]
  (let [search-text (ins/use-search-text)]
    [highlight-clj (pr-str (f/filter-value value search-text))]))

(defn inspect-code
  ([code]
   (inspect-code nil code))
  ([attrs code]
   (let [theme    (theme/use-theme)
         opts     (ins/use-options)
         code     (if-let [search-text (ins/use-search-text)]
                    (->> (str/split-lines code)
                         (filter
                          (fn [line-content]
                            (some
                             #(str/includes? line-content %)
                             (str/split search-text #"\s+"))))
                         (str/join "\n"))
                    code)
         out      (if-let [language (or (:class attrs)
                                        (some->
                                         (get-in opts [:props :portal.viewer/code :language])
                                         name))]
                    (hljs/highlight
                     code
                     #js {:language (get language-map language language)})
                    (hljs/highlightAuto code))
         html     (.-value out)
         language (or (:class attrs) (.-language out))]
     [s/div
      {:class root-class
       :title language
       :style
       {:overflow      :auto
        :position      :relative
        :box-sizing    :border-box
        :padding       (* 1.5 (:padding theme))
        :border        [1 :solid (::c/border theme)]
        :background    (ins/get-background)
        :border-radius (:border-radius theme)
        :max-height    (when-not (:expanded? opts) "24rem")}}
      [:pre
       {:on-click
        (fn [e]
          (when-not (=  "" (.toString (.getSelection js/window)))
            (.stopPropagation e)))
        :style                   {:padding     0
                                  :margin      0
                                  :background  :none
                                  :width       :fit-content
                                  :color       (::c/text theme)
                                  :font-family (:font-family theme)
                                  :font-size   (:font-size theme)}}
       [h/html+ html]]])))

(def viewer
  {:predicate string?
   :component #'inspect-code
   :name      :portal.viewer/code})

(def pr-str-viewer
  {:predicate (constantly true)
   :component #'inspect-pr-str
   :name      :portal.viewer/pr-str})

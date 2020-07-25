(ns portal.viewer.markdown
  (:require [hickory.core :refer [parse-fragment as-hiccup]]
            [hickory.utils :as utils]
            [markdown.common :as common]
            [markdown.core :refer [md->html]]
            [portal.viewer.hiccup :refer [inspect-hiccup]]))

(defn inspect-markdown [settings value]
  ;; I couldn't figure out a good way to disable html escaping, which
  ;; occurs in both markdown-clj and hickory, so I decided to manually
  ;; intercepts calls into utility methods and replace their
  ;; implementations. This is probably brittle, but I have little choice.
  (with-redefs
   [common/escape-code   identity
    common/escaped-chars identity
    utils/html-escape    identity]
    [inspect-hiccup
     settings
     (->> (md->html value)
          parse-fragment
          (map as-hiccup)
          (into [:div {:style {:max-width "1012px"
                               :margin "0 auto"}}]))]))

(ns portal.ssr.ui.core
  (:require ["idiomorph" :as i]
            [portal.ui.rpc :as rpc]))

(defn render [html]
  (i/morph (.getElementById js/document "root")
           (str "<pre>" (pr-str html) "</pre>")
           #js {:morphStyle "innerHTML"}))

(defn main! []
  (rpc/connect {:path "/ssr"
                :on-message
                (fn [data]
                  (render data))})
  (render "<h1>hello, worlds</h1>"))
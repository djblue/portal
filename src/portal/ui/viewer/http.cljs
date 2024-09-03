(ns ^:no-doc portal.ui.viewer.http
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [portal.colors :as c]
            [portal.ui.filter :as-alias f]
            [portal.ui.inspector :as ins]
            [portal.ui.select :as select]
            [portal.ui.styled :as d]
            [portal.ui.theme :as theme]))

;;; :spec
(s/def ::uri string?)

(defn valid-status? [value] (<= 100 value 599))
(s/def ::status (s/and int? valid-status?))
(s/def ::name (s/or :string string? :keyword keyword?))
(s/def ::header (s/or :string string? :strings (s/coll-of string?)))
(s/def ::headers (s/map-of ::name ::header))
(s/def ::query-params (s/map-of ::name ::name))
(s/def ::doc string?)

(s/def ::request-method #{:get :head :post :put :patch :delete :options})

(s/def ::request
  (s/keys :req-un [::request-method
                   ::uri]
          :opt-un [::headers
                   ::query-params
                   ::body
                   ::doc]))

(s/def ::request
  (s/keys :req-un [::request-method
                   ::uri]
          :opt-un [::headers
                   ::query-params
                   ::body
                   ::doc]))

(s/def ::response
  (s/keys :req-un [::status]
          :opt-un [::headers
                   ::body
                   ::doc]))
;;;

(def ^:private method->color
  {:get     ::c/boolean
   :head    ::c/number
   :put     ::c/uri
   :post    ::c/string
   :patch   ::c/tag
   :delete  ::c/exception
   :options ::c/package})

(defn inspect-http-request [value]
  (let [theme      (theme/use-theme)
        opts       (ins/use-options)
        background (ins/get-background)
        method     (:request-method value)
        color      (-> method method->color theme)
        expanded?  (:expanded? opts)]
    [d/div
     [d/div
      {:style
       {:display     :flex
        :align-items :stretch
        :background  background}}
      [d/div
       {:style
        {:cursor     :pointer
         :color      background
         :padding    [(* 0.5 (:padding theme)) (* 1.5 (:padding theme))]
         :background color
         :border     [1 :solid color]

         :border-top-left-radius (:border-radius theme)
         :border-bottom-left-radius
         (when-not expanded? (:border-radius theme))}}
       (str/upper-case (name method))]
      [d/div
       {:style
        {:flex    "1"
         :display :flex
         :gap     (:padding theme)
         :padding [(* 0.5 (:padding theme)) (:padding theme)]
         :border  [1 :solid (::c/border theme)]

         :border-bottom-style     (when expanded? :none)
         :border-top-right-radius (:border-radius theme)
         :border-bottom-right-radius
         (when-not expanded? (:border-radius theme))}}
       [ins/toggle-expand]
       [select/with-position
        {:row -1 :column 0}
        [ins/with-key
         :uri
         [ins/inspector (:uri value)]]]]]
     (when (:expanded? opts)
       [ins/inspect-map-k-v (dissoc value :uri :request-method)])]))

(defn- status->color [status]
  (cond
    (<= 100 status 199) ::c/boolean
    (<= 200 status 299) ::c/string
    (<= 300 status 399) ::c/tag
    (<= 400 status 499) ::c/uri
    (<= 500 status 599) ::c/exception))

(defn inspect-http-response [value]
  (let [theme        (theme/use-theme)
        opts         (ins/use-options)
        expanded?    (:expanded? opts)
        background   (ins/get-background)
        content-type (or (get-in value [:headers "Content-Type"])
                         (get-in value [:headers :content-type]))
        status       (:status value)
        color        (-> status status->color theme)]
    [d/div
     [d/div
      {:style
       {:display    :flex
        :background background}}
      [d/div
       {:style
        {:cursor                 :pointer
         :background             color
         :padding                [(* 0.5 (:padding theme)) (* 1.5 (:padding theme))]
         :color                  background
         :border                 [1 :solid color]
         :border-top-left-radius (:border-radius theme)
         :border-bottom-left-radius
         (when-not expanded? (:border-radius theme))}}
       status]
      [d/div
       {:style
        {:flex                    "1"
         :display                 :flex
         :gap                     (:padding theme)
         :padding                 (* 0.5 (:padding theme))
         :border                  [1 :solid (::c/border theme)]
         :border-bottom-style     (when expanded? :none)
         :border-top-right-radius (:border-radius theme)
         :border-bottom-right-radius
         (when-not expanded? (:border-radius theme))}}
       [ins/toggle-expand]
       [select/with-position
        {:row -1 :column 0}
        [ins/with-key
         :headers
         [ins/with-key
          "Content-Type"
          [ins/inspector content-type]]]]]]
     (when (:expanded? opts)
       [ins/inspect-map-k-v (dissoc value :status)])]))

(defn get-component [value]
  (cond
    (s/valid? ::request value)
    inspect-http-request

    (s/valid? ::response value)
    inspect-http-response))

(defn inspect-http [value]
  (let [component (get-component value)]
    [component value]))

(def viewer
  {:predicate get-component
   :component #'inspect-http
   :name      :portal.viewer/http
   :doc       "Highlight HTTP method and status code for http request and response."})

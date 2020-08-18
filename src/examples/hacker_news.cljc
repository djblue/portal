(ns examples.hacker-news
  (:require #?(:clj [cheshire.core :as json])
            [clojure.core.protocols :refer [nav]]
            #?(:clj  [portal.sync  :as a]
               :cljs [portal.async :as a])
            #?(:cljs [examples.fetch :refer [fetch]])))

(def root "https://hacker-news.firebaseio.com/v0")

(def item-doc
  {:id          "The item's unique id."
   :deleted     "true if the item is deleted."
   :type        "The type of item. One of \"job\", \"story\", \"comment\", \"poll\", or \"pollopt\"."
   :by          "The username of the item's author."
   :time        "Creation date of the item, in Unix Time."
   :text        "The comment, story or poll text. HTML."
   :dead        "true if the item is dead."
   :parent      "The comment's parent: either another comment or the relevant story."
   :poll        "The pollopt's associated poll."
   :kids        "The ids of the item's comments, in ranked display order."
   :url         "The URL of the story."
   :score       "The story's score, or the votes for a pollopt."
   :title       "The title of the story, poll or job. HTML."
   :parts       "A list of related pollopts, in display order."
   :descendants "In the case of stories or polls, the total comment count."})

(def user-doc
  {:id        "The user's unique username. Case-sensitive. Required."
   :delay     "Delay in minutes between a comment's creation and its visibility to other users."
   :created   "Creation date of the user, in Unix Time."
   :karma     "The user's karma."
   :about     "The user's optional self-description. HTML."
   :submitted "List of the user's stories, polls and comments."})

(defn fetch-json [url]
  #?(:clj  (-> url slurp (json/parse-string true))
     :cljs (-> (fetch url)
               (.then #(js/JSON.parse %))
               (.then #(js->clj % :keywordize-keys true)))))

(defn as-url [s]
  #?(:clj (java.net.URL. s) :cljs (js/URL. s)))

(defn as-date [s]
  #?(:clj (java.util.Date. s) :cljs (js/Date. s)))

(defn fetch-hn [path]
  (a/let [url   (as-url (str root path))
          res   (fetch-json url)
          item  (with-meta res {:hacker-news/api-url url})]
    (if-not (map? item)
      item
      (cond-> item
        (contains? item :url)
        (update :url #(as-url %))

        (contains? item :time)
        (update :time #(as-date (* % 1000)))

        (contains? item :created)
        (update :created #(as-date (* % 1000)))

        (contains? item :type)
        (update :type keyword)))))

(declare nav-hn)

(defn fetch-user [user]
  (a/let [res (fetch-hn (str "/user/" user ".json"))]
    (vary-meta res assoc `nav #'nav-hn)))

(defn nav-item [_coll _k v]
  (a/let [res (fetch-hn (str "/item/" v ".json"))]
    (vary-meta res assoc `nav #'nav-hn)))

(def stories
  (with-meta
    #{:topstories :newstories :beststories
      :askstories :showstories :jobstories}
    {`nav #'nav-hn}))

(defn fetch-stories [type]
  (a/let [res (fetch-hn (str "/" (name type) ".json"))]
    (vary-meta (take 15 res) assoc `nav #'nav-item)))

(defn nav-hn [coll k v]
  (cond
    (stories v)             (fetch-stories v)
    (keyword? v)            (get (if (contains? coll :type)
                                   item-doc
                                   user-doc) v v)
    (= k :by)               (fetch-user v)
    (= k :parent)           (nav-item coll k v)
    (#{:kids :submitted} k) (vary-meta v assoc `nav #'nav-item)
    :else v))


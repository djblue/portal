(ns portal.ssr.hiccup
  (:require
   [portal.ssr.ui.react :as react]
   [portal.ui.styled :as d])
  (:import
   [java.io ByteArrayInputStream]
   [java.nio ByteBuffer CharBuffer]
   [java.nio.charset StandardCharsets]))

(defn ->input-stream [^bytes bs n] (ByteArrayInputStream. bs 0 n))

(def ^:dynamic *handlers* nil)

(def ^:private handler-attributes
  #{:on-click
    :on-double-click
    :on-mouse-up
    :on-mouse-down
    :on-mouse-over
    :on-mouse-enter
    :on-mouse-leave
    :on-focus
    :on-change
    :on-visible
    :on-key-down})

(defn- extract-handlers! [id attrs]
  (let [handlers *handlers*]
    (reduce-kv
     (fn [_ attr handler]
       (when (contains? handler-attributes attr)
         (vswap! handlers update id assoc attr handler)))
     nil
     attrs)))

(defn- attrs! [write! m]
  (reduce-kv
   (fn [_ k v]
     (cond
       (= :disabled k)
       (when v (write! " disabled"))

       (= :auto-focus k)
       (write! " autofocus")

       (handler-attributes k)
       (do (write! " data-")
           (write! (name k)))

       :else
       (when (some? v)
         (write! " ")
         (write! (name k))
         (write! "=\"")
         (write! (str v))
         (write! "\""))))
   nil
   m))

(def ^:private self-closing?
  #{:area
    :base
    :br
    :col
    :embed
    :hr
    :img
    :input
    :link
    :meta
    :param
    :source
    :track
    :wbr})

(defn- display-none? [hiccup]
  (and (map? (second hiccup))
       (= :none (get-in hiccup [1 :style :display]))))

(defn- html* [write! hiccup]
  (cond
    (or (list? hiccup) (seq? hiccup))
    (doseq [h hiccup] (html* write! h))

    (and (vector? hiccup) (keyword? (first hiccup)))
    (when-not (display-none? hiccup)
      (let [[tag & args] hiccup
            attrs (when (map? (first args)) (first args))
            children (cond-> args (map? (first args)) rest)
            element-id (::react/id (meta hiccup))]
        (extract-handlers! element-id attrs)
        (if (= :<> tag)
          (doseq [h children] (html* write! h))
          (let [tag-name (name tag)]
            (if (self-closing? tag)
              (do
                (write! "<")
                (write! tag-name)
                (when element-id
                  (write! " id=\"")
                  (write! (str element-id))
                  (write! "\""))
                (when attrs
                  (attrs! write! (d/attrs->css attrs)))
                (write! "/>"))
              (do
                (write! "<")
                (write! tag-name)
                (when element-id
                  (write! " id=\"")
                  (write! (str element-id))
                  (write! "\""))
                (when attrs
                  (attrs! write! (d/attrs->css attrs)))
                (write! ">")
                (doseq [h children] (html* write! h))
                (write! "</")
                (write! tag-name)
                (write! ">")))))))

    :else (write! (str hiccup))))

(defn html! [^bytes out hiccup]
  (try
    (let [buffer  (ByteBuffer/wrap out)
          encoder (.newEncoder StandardCharsets/UTF_8)
          write!  (fn write! [^String s]
                    (let [char-buffer (CharBuffer/wrap s)]
                      (.encode encoder char-buffer buffer true)))]
      (html* write! hiccup)
      (.flush encoder buffer)
      (.position buffer))
    (catch Exception e (tap> e))))

(defn html-str [hiccup]
  (with-out-str (html* print hiccup)))

(comment
  (let [buffer (byte-array 1024)
        n (html! buffer [:div {:hello "world"}])]
    (->input-stream buffer n))
  (html-str [:div {}])
  comment)
(ns ^:no-doc portal.runtime.jvm.hiccup
  (:require
   [portal.runtime.react :as react])
  (:import
   [java.io ByteArrayInputStream]
   [java.nio ByteBuffer]
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

(defn- html* [write! hiccup]
  (cond
    (or (list? hiccup) (seq? hiccup))
    (doseq [h hiccup] (html* write! h))

    (and (vector? hiccup) (keyword? (nth hiccup 0)))
    (let [tag        (nth hiccup 0)
          has-attrs? (map? (nth hiccup 1 nil))
          attrs      (when has-attrs? (nth hiccup 1))
          element-id (::react/id (meta hiccup))
          attrs      (cond-> attrs element-id (assoc :id element-id))]
      (extract-handlers! element-id attrs)
      (if (= :<> tag)
        (let [start (if has-attrs? 2 1)
              cnt   (count hiccup)]
          (loop [i start]
            (when (< i cnt)
              (html* write! (nth hiccup i))
              (recur (unchecked-inc i)))))
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
                (attrs! write! attrs))
              (write! "/>"))
            (do
              (write! "<")
              (write! tag-name)
              (when element-id
                (write! " id=\"")
                (write! (str element-id))
                (write! "\""))
              (when attrs
                (attrs! write! attrs))
              (write! ">")
              (let [start (if has-attrs? 2 1)
                    cnt   (count hiccup)]
                (loop [i start]
                  (when (< i cnt)
                    (html* write! (nth hiccup i))
                    (recur (unchecked-inc i)))))
              (write! "</")
              (write! tag-name)
              (write! ">"))))))

    :else (write! hiccup)))

(defn html! [^bytes out hiccup]
  (try
    (let [buffer (ByteBuffer/wrap out)
          write! (fn write! [^String s]
                   (.put buffer (.getBytes s StandardCharsets/UTF_8)))]
      (html* write! hiccup)
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
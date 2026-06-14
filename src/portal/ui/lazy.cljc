(ns ^:no-doc portal.ui.lazy
  #?(:clj (:refer-clojure :exclude [lazy-seq random-uuid]))
  #?(:clj (:require
           [portal.colors :as c]
           [portal.runtime.polyfill :refer [random-uuid]]
           [portal.ui.icons :as icons]
           [portal.ui.react :as react]
           [portal.ui.select :as select]
           [portal.ui.state :as state]
           [portal.ui.styled :as d]
           [portal.ui.theme :as theme])))

(def count-limit 100000)

(defn safe-count
  ([coll]
   (safe-count coll count-limit))
  ([coll count-limit]
   (if (counted? coll)
     [(count coll) false]
     (let [n (count (take (inc count-limit) coll))
           truncated? (> n count-limit)]
       [(if truncated? count-limit n) truncated?]))))

(defn safe-count-str [coll]
  (let [[n truncated?] (safe-count coll)]
    (if truncated? (str count-limit "+") (str n))))

(defn- lazy-seq? [value]
  #?(:clj (or (instance? clojure.lang.LazySeq value)
              (instance? clojure.lang.Iterate value)
              (instance? clojure.lang.Range value))
     :cljs (or (instance? cljs.core/LazySeq value)
               (instance? cljs.core/Iterate value)
               (instance? cljs.core/Range value))))

(defn safe-seq [value]
  (cond->> value (lazy-seq? value) (take count-limit)))

#?(:clj
   (defn lazy-render-pre [coll {:keys [default-take step style context]
                                :or   {default-take 0 step 10}}]
     (let [theme (theme/use-theme)
           [n set-n!]  (react/use-state default-take)
           [head tail] (split-at (or n default-take) (reverse coll))]
       [:<>
        (when (seq tail)
          [d/button
           {:style
            (merge
             {:width "100%"
              :cursor :pointer
              :color (::c/border theme)
              :padding (* 0.5 (:padding theme))
              :background ((if (:alt-bg (meta context)) ::c/background ::c/background2) theme)
              :border-radius (:border-radius theme)
              :border [1 :solid (::c/border theme)]}
             style)
            :style/hover (when-let [depth (:depth context)]
                           {:border [1 :solid (get theme (nth theme/order (inc depth)))]})
            :style/focus (when-let [depth (:depth context)]
                           {:outline :none
                            :border [1 :solid (get theme (nth theme/order (inc depth)))]})
            :on-click (fn [_]
                        (set-n! (+ n step)))}
           [icons/ellipsis-h]])
        (reverse head)])))

#?(:clj
   (defn lazy-render-post [coll {:keys [default-take step style context auto]
                                 :or   {default-take 0 step 10}}]
     (let [theme (theme/use-theme)
           [n set-n!]  (react/use-state default-take)
           [head tail] (split-at (or n default-take) coll)]
       [:<>
        head
        (when (seq tail)
          (if auto
            [:visible-sensor
             {:id (random-uuid)
              :on-visible (fn [_] (set-n! (+ n step)))}]
            [d/button
             {:style
              (merge
               {:cursor :pointer
                :color (::c/border theme)
                :padding (* 0.5 (:padding theme))
                :background ((if (:alt-bg (meta context)) ::c/background ::c/background2) theme)
                :border-radius (:border-radius theme)
                :border [1 :solid (::c/border theme)]}
               style)
              :style/hover (when-let [depth (:depth context)]
                             {:border [1 :solid (get theme (nth theme/order (inc depth)))]})
              :style/focus (when-let [depth (:depth context)]
                             {:outline :none
                              :border [1 :solid (get theme (nth theme/order (inc depth)))]})
              :on-click (fn [_]
                          (set-n! (+ n step)))}
             [icons/ellipsis-h]]))])))

#?(:clj
   (defn lazy-seq-auto
     ([coll]
      (lazy-seq-auto coll nil))
     ([coll {:keys [default-take step]
             :or   {default-take 0 step 10}}]
      (let [theme (theme/use-theme)
            default-take (::default-take theme default-take)
            [n set-n!] (react/use-state default-take)
            [head tail] (split-at n coll)]
        [:<>
         head
         (when (seq tail)
           [:visible-sensor
            {:id (random-uuid)
             :on-visible (fn [_] (set-n! (+ n step)))}])]))))

#?(:clj
   (defn lazy-seq
     ([coll]
      (lazy-seq coll nil))
     ([coll {:keys [context] :as opts}]
      (let [state (state/use-state)
            focus (react/use-atom state state/get-focus-context)
            contains-focus? (state/contains-context? context focus)
            current-position (select/use-position)
            index (when contains-focus?
                    (let [selected-position (::select/position (meta focus))]
                      (first
                       (keep-indexed
                        (fn [index node]
                          (some
                           (fn [position]
                             (when (= position
                                      (first (drop (count current-position) selected-position)))
                               index))
                           (select/find-positions node)))
                        coll))))]
        (if-not contains-focus?
          [lazy-seq-auto coll opts]
          (let [pre  (take (or index 0) coll)
                post (drop (if index (inc index) 0) coll)]
            [:<>
             [lazy-render-pre pre (assoc opts :default-take 5)]
             (when index (nth coll index))
             [lazy-render-post post (assoc opts :default-take 5)]]))))))

#?(:clj
   (defn use-visible []
     (let [[visible? set-visible!] (react/use-state false)]
       [(when-not visible?
          [:visible-sensor
           {:id (random-uuid)
            :on-visible (fn [_] (set-visible! true))}])
        visible?])))

#?(:clj
   (defn lazy-render [child]
     (let [[show set-show!] (react/use-state false)]
       (if show
         child
         [:<>
          [:visible-sensor
           {:on-visible (fn [_] (set-show! true))}]
          [:div {:style {:height "50vh"}}]]))))
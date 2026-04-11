(ns portal.runtime.react-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [portal.runtime.react :as r]))

(deftest basic-hiccup-test
  (is (= [:div {} "hello, world"]
         (r/render [:div "hello, world"]))))

(defn- basic-componenet [text] [:h1 {} text])

(defn- two-arity-component [a b]
  [:<> [:span a] [:span b]])

(deftest basic-component-test
  (testing "top level component"
    (is (= [:h1 {} "hello, world"]
           (r/render [basic-componenet "hello, world"]))))
  (testing "multiple parameters component"
    (is (= [:<> {} [:span {} "hello"] [:span {} "world"]]
           (r/render [two-arity-component "hello" "world"]))))
  (testing "nested in hiccup"
    (is (= [:div {} [:h1 {} "hello, world"]]
           (r/render [:div [basic-componenet "hello, world"]])))))

(defn bad-component [] :foo)

;; (deftest component-invariants-test
;;   (is (thrown? #?(:clj  clojure.lang.ExceptionInfo
;;                   :cljr clojure.lang.ExceptionInfo
;;                   :cljs cljs.core/ExceptionInfo)
;;                (r/render [bad-component]))))

(deftest hook-invariants-test
  (is (thrown? #?(:clj  clojure.lang.ExceptionInfo
                  :cljr clojure.lang.ExceptionInfo
                  :cljs cljs.core/ExceptionInfo)
               (r/use-state 0)))
  (is (thrown? #?(:clj  clojure.lang.ExceptionInfo
                  :cljr clojure.lang.ExceptionInfo
                  :cljs cljs.core/ExceptionInfo)
               (r/use-effect (fn []))))
  (is (thrown? #?(:clj  clojure.lang.ExceptionInfo
                  :cljr clojure.lang.ExceptionInfo
                  :cljs cljs.core/ExceptionInfo)
               (r/use-effect (fn []) []))))

(defn- state-component []
  (let [[state set-state!] (r/use-state 0)]
    [:div {:on-click #(set-state! inc)} (str state)]))

(deftest state-re-render
  (let [output (r/render [state-component])
        vdom (meta output)
        on-click (get-in output [1 :on-click])]
    (on-click)
    (is (= "1" (last (r/render vdom [state-component]))))))

(defn- state-2-component []
  (let [[static _] (r/use-state 0)
        [state set-state!] (r/use-state 1)]
    [:div {:on-click #(set-state! inc)} (str static) (str state)]))

(deftest multi-state-re-render-test
  (let [output (r/render [state-2-component])
        vdom (meta output)
        on-click (get-in output [1 :on-click])]
    (on-click)
    (is (= ["0" "2"]
           (rest (rest (r/render vdom [state-2-component])))))))

(defn- effect-component [{:keys [on-mount on-unmount deps]} & children]
  (r/use-effect
   (fn []
     (on-mount)
     (fn []
       (on-unmount)))
   deps)
  (into [:<>] children))

(deftest use-effect-test
  (let [mount (atom 0)
        un-mount (atom 0)
        props {:on-mount   #(swap! mount inc)
               :on-unmount #(swap! un-mount inc)}
        output (r/render [effect-component props])
        vdom (meta output)]
    (r/render vdom nil)
    (is (== 1 @mount))
    (is (== 1 @un-mount))))

(deftest use-effect-stable-deps-test
  (let [mount (atom 0)
        un-mount (atom 0)
        props {:on-mount   #(swap! mount inc)
               :on-unmount #(swap! un-mount inc)
               :deps []}
        output (r/render [effect-component props])
        vdom (meta output)]
    (r/render vdom [effect-component props])
    (is (== 1 @mount))
    (is (== 0 @un-mount))))

(deftest component-unmount-test
  (let [mount (atom 0)
        un-mount (atom 0)
        props {:on-mount   #(swap! mount inc)
               :on-unmount #(swap! un-mount inc)
               :deps []}
        output (r/render [effect-component props])
        vdom (meta output)]
    (r/render vdom nil)
    (is (== 1 @mount))
    (is (== 1 @un-mount))))

(deftest nested-component-unmount-test
  (let [mount (atom 0)
        un-mount (atom 0)
        props {:on-mount   #(swap! mount inc)
               :on-unmount #(swap! un-mount inc)
               :deps []}
        output (r/render [effect-component props
                          [effect-component props]])
        vdom (meta output)]
    (r/render vdom nil)
    (is (== 2 @mount))
    (is (== 2 @un-mount))))

(deftest preserve-effect-on-re-render-test
  (let [un-mount (atom 0)
        props {:on-mount (fn [])
               :on-unmount #(swap! un-mount inc)
               :deps [:foo]}
        output (r/render [effect-component props])
        vdom   (meta output)
        output (r/render vdom [effect-component (assoc props :x :y)])
        vdom   (meta output)]
    (r/render vdom [effect-component (assoc props :deps [:bar])])
    (is (== 1 @un-mount))))

(defn atom-component [a]
  [:span (pr-str (r/use-atom a))])

(deftest atom-component-test
  (let [a (atom 0)
        output (r/render [atom-component a])
        _ (swap! a inc)
        vdom (meta output)]
    (is (= [:span {} "1"]
           (r/render vdom [atom-component a])))))

(defn- list-component []
  [:<> (for [n (range 0 1)] [:span {:key n} (str n)])])

(deftest list-component-test
  (is (= [:<> {} (list [:span {:key 0} "0"])]
         (r/render [list-component]))))

(def ^:private context (r/create-context "initial-value"))

(defn- context-component []
  [:div (r/use-context context)])

(deftest context-component-test
  (testing "initial context value"
    (is (= [:div {} "initial-value"]
           (r/render [context-component]))))
  (testing "simple context usage"
    (let [{:keys [provider]} context]
      (is (= [:div {} "hello, world"]
             (r/render
              [provider {:value "hello, world"}
               [context-component]]))))))

(deftest nested-context-test
  (testing "indirect context usage"
    (let [{:keys [provider]} context]
      (is (= [:div {} [:div {} "hello, world"]]
             (r/render
              [provider {:value "hello, world"}
               [:div
                [context-component]]])))))
  (testing "nested shadow context"
    (let [{:keys [provider]} context]
      (is (= [:div {} "bottom"]
             (r/render
              [provider {:value "top"}
               [provider {:value "middle"}
                [provider {:value "bottom"}
                 [context-component]]]]))))))

(defn- list-context-component []
  [:<> (for [k (range 0 1)] ^{:key k} [context-component])])

(deftest list-context-test
  (let [{:keys [provider]} context]
    (is (=  [:<> {} (list [:div {} "hello, world"])]
            (r/render
             [provider {:value "hello, world"}
              [list-context-component]])))))

(defn stateless-component []
  [:div {:on-click (fn [])} "hello"])

(deftest stable-re-render
  (let [a (r/render [stateless-component])
        b (r/render (meta a) [stateless-component])]
    (is (= a b))))

(defn unmount-component [{:keys [render unmount]}]
  (swap! render inc)
  (r/use-effect
   (fn []
     #(swap! unmount inc))
   [])
  nil)

(deftest unmount-on-key-change []
  (let [render  (atom 0)
        unmount (atom 0)
        props {:render  render
               :unmount unmount}
        output (r/render      ^{:key 0} [unmount-component props])
        _ (is (== 1 @render))
        _ (is (== 0 @unmount))
        vdom (meta output)
        output (r/render vdom ^{:key 0} [unmount-component props])
        _ (is (== 1 @render))
        _ (is (== 0 @unmount))
        vdom (meta output)
        output (r/render vdom ^{:key 0} [unmount-component (assoc props :key 1)])
        _ (is (== 2 @render))
        _ (is (== 1 @unmount))]
    output))

(deftest sorted-map-element-key
  (is (nil? (#'r/element-key [:div (sorted-map 'key :value)])))
  (is (= :value (#'r/element-key [:div (sorted-map :key :value)]))))
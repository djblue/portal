(ns portal.ui.macros
  #?(:cljs (:require-macros portal.ui.macros)))

(defn- dispatch [_this] #?(:cljs (.-dispatch _this)))

(defmulti on-connect #'dispatch)
(defmulti on-disconnect #'dispatch)
(defmulti on-attribute-changed #'dispatch)

(defmethod on-connect :default [_this])
(defmethod on-disconnect :default [_this])
(defmethod on-attribute-changed :default [_this _attr _old _new])

#?(:cljs
   (defn- webcomponent! [component-name observed-attributes]
     (let [component
           (fn component []
             (let [e (.construct js/Reflect js/HTMLElement #js [] component)]
               (set! (.-dispatch e) component-name)
              ;;  (set! (.-shadow e) (.attachShadow e #js {:mode "open"}))
               e))]
       (set! (.-prototype component)
             (.create js/Object (.-prototype js/HTMLElement)
                      #js {:connectedCallback
                           #js {:configurable true
                                :value        (fn []
                                                (this-as this (on-connect this)))}
                           :disconnectedCallback
                           #js {:configurable true
                                :value        (fn []
                                                (this-as this (on-disconnect this)))}
                           :attributeChangedCallback
                           #js {:configurable true
                                :value        (fn [attr old-val new-val]
                                                (this-as this (on-attribute-changed this attr old-val new-val)))}}))
       (when (seq observed-attributes)
         (.defineProperty js/Object component "observedAttributes"
                          #js {:get (fn [] (clj->js observed-attributes))}))
       (js/window.customElements.define component-name component)
       component)))

(defmacro defcomponent [component-name observed-attributes & component-methods]
  `(do
     (defonce ~component-name
       (do (webcomponent! ~(name component-name) '~observed-attributes)
           ~(keyword component-name)))
     ~@(for [[component-method params & body] component-methods]
         `(defmethod
            ~(symbol "portal.ui.macros" (str component-method))
            ~(name component-name)
            ~params
            (let [~@(mapcat
                     (fn [attribute] [attribute `(.getAttribute ~(first params) ~(name attribute))])
                     observed-attributes)]
              ~@body)))
     (keyword ~component-name)))
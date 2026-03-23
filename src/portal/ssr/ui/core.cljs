(ns portal.ssr.ui.core
  (:require
   [clojure.string :as str]))

(require '["Idiomorph" :as i])

(defn webcomponent! [name {:keys [on-connect on-disconnect]}]
  (let [component
        (fn component []
          (let [e (.construct js/Reflect js/HTMLElement #js [] component)]
            (set! (.-shadow e) (.attachShadow e #js {:mode "open"}))
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
                                             (this-as this (on-disconnect this)))}}))
    (js/window.customElements.define name component)
    component))

(defn- observer-visible? [entries]
  (< 0.5 (reduce
          (fn [sum entry]
            (if-not (.-isIntersecting entry)
              sum
              (+ sum (.-intersectionRatio entry)))) 0 entries)))

(defn render [html]
  (i/morph (.getElementById js/document "root")
           html
           #js {:morphStyle "innerHTML"
                :ignoreActiveValue true
                :callbacks
                #js {:afterNodeAdded
                     (fn [node]
                       (when (.-querySelectorAll node)
                         (when-let [autofocus (first (.querySelectorAll node "[autofocus]"))]
                           (.focus autofocus))))}}))

(defn- parent-elements [el]
  (take-while some? (iterate (fn [^js el] (.-parentElement el)) el)))

(defmulti on-message :op)

(defmethod on-message "on-styles" [{:keys [append-styles]}]
  (doseq [style append-styles
          :let [el (.createElement js/document "style")]]
    (set! (.-innerHTML el) style)
    (.appendChild js/document.head el)))

(defmethod on-message "on-render" [{:keys [html]}]
  (render html))

(defn- copy-to-clipboard! [s]
  (let [el (js/document.createElement "textarea")]
    (set! (.-value el) s)
    (js/document.body.appendChild el)
    (.select el)
    (js/document.execCommand "copy")
    (js/document.body.removeChild el)))

(defmethod on-message "on-copy" [{:keys [text]}]
  (copy-to-clipboard! text))

(defn find-handler-1 [el handler]
  (when (.getAttribute el handler) (.getAttribute el "id")))

(defn find-handler [el handler]
  (some
   (fn [^js el]
     (find-handler-1 el handler))
   (parent-elements el)))

(defn on-connect [ws]
  (let [root (.getElementById js/document "root")]
    (webcomponent!
     "visible-sensor"
     {:on-connect
      (fn [this]
        (let [observer
              (js/IntersectionObserver.
               (fn [entries]
                 (when (observer-visible? entries)
                   (.unobserve (.-observer this) this)
                   (.send ^js ws
                          (.stringify
                           js/JSON
                           #js {:op "on-visible" :id (find-handler-1 this "data-on-visible")}))))
               #js {:root nil :rootMargin "0px" :threshold 0.5})]
          (set! (.-observer this) observer)
          (.observe observer this)))
      :on-disconnect
      (fn [this]
        (.unobserve (.-observer this) this))})
    (.addEventListener
     root "focusin"
     (fn [^js e]
       (when-let [id (find-handler (.-srcElement e) "data-on-focus")]
         (.send ^js ws
                (.stringify
                 js/JSON
                 #js {:op "on-focus" :id id})))))
    (.addEventListener
     root "mouseover"
     (fn [^js e]
       (when-let [id (find-handler (.-srcElement e) "data-on-mouse-over")]
         (.send ^js ws
                (.stringify
                 js/JSON
                 #js {:op "on-mouse-over" :id id})))
       (let [target (.-target e)]
         (when-let [id (find-handler-1 target "data-on-mouse-enter")]
           (when-not (.contains target (.-relatedTarget e))
             (.send ^js ws
                    (.stringify
                     js/JSON
                     #js {:op "on-mouse-enter" :id id})))))))
    (.addEventListener
     root "mouseout"
     (fn [^js e]
       (let [target (.-target e)]
         (when-let [id (find-handler-1 target "data-on-mouse-leave")]
           (when-not (.contains target (.-relatedTarget e))
             (.send ^js ws
                    (.stringify
                     js/JSON
                     #js {:op "on-mouse-leave" :id id})))))))
    (.addEventListener
     js/window "input"
     (fn [^js e]
       (when-let [id (find-handler (.-srcElement e) "data-on-change")]
         (.send ^js ws
                (.stringify
                 js/JSON
                 #js
                  {:op "on-change"
                   :id id
                   :target #js {:value (.. e -target -value)}})))))
    (.addEventListener
     js/window "keydown"
     (fn [^js e]
       (when (and (.-ctrlKey e)
                  (#{"j" "p"} (.-key e)))
         (.preventDefault e))
       (.send ^js ws
              (.stringify
               js/JSON
               (if-let [id (find-handler (.-srcElement e) "data-on-key-down")]
                 #js {:op "on-key-down"
                      :id id
                      :key (.toLowerCase (.-key e))
                      :ctrl-key (.-ctrlKey e)
                      :meta-key (.-metaKey e)
                      :shift-key (.-shiftKey e)
                      :alt-key (.-altKey e)}
                 #js {:op "on-key-down"
                      :key (.toLowerCase (.-key e))
                      :ctrl-key (.-ctrlKey e)
                      :meta-key (.-metaKey e)
                      :shift-key (.-shiftKey e)
                      :alt-key (.-altKey e)})))))
    (.addEventListener
     root "mouseup"
     (fn [^js e]
       (when-let [id (find-handler (.-srcElement e) "data-on-mouse-up")]
         (.send ^js ws
                (.stringify
                 js/JSON
                 #js {:op "on-mouse-up"
                      :id id
                      :button (.-button e)
                      :ctrl-key (.-ctrlKey e)
                      :meta-key (.-metaKey e)
                      :shift-key (.-shiftKey e)
                      :alt-key (.-altKey e)})))))
    (.addEventListener
     root "click"
     (fn [^js e]
       (when-let [id (find-handler (.-srcElement e) "data-on-click")]
         (.send ^js ws
                (.stringify
                 js/JSON
                 #js {:op "on-click"
                      :id id
                      :ctrl-key (.-ctrlKey e)
                      :meta-key (.-metaKey e)
                      :shift-key (.-shiftKey e)
                      :alt-key (.-altKey e)})))))
    (.addEventListener
     root "dblclick"
     (fn [^js e]
       (when-let [id (find-handler (.-srcElement e) "data-on-double-click")]
         (.send ^js ws
                (.stringify
                 js/JSON
                 #js {:op "on-double-click"
                      :id id
                      :ctrl-key (.-ctrlKey e)
                      :meta-key (.-metaKey e)
                      :shift-key (.-shiftKey e)
                      :alt-key (.-altKey e)})))))))

(defn connect [{:keys [protocol host port
                       path session
                       on-message]}]
  (js/Promise.
   (fn [resolve reject]
     (when-let [chan (js/WebSocket.
                      (str protocol "//" host (when port (str ":" port)) path "?" session))]
       (set! (.-onmessage chan) #(on-message (.-data %)))
       (set! (.-onerror chan)   (fn [e]
                                  (reject e)))
       ;;  (set! (.-onclose chan)   #(reset!  ws-promise nil))
       (set! (.-onopen chan)    (resolve chan))))))

(defn- get-session []
  (if (exists? js/PORTAL_SESSION)
    js/PORTAL_SESSION
    (subs js/window.location.search 1)))

(defn- get-host []
  (if (exists? js/PORTAL_HOST)
    (first (str/split js/PORTAL_HOST #":"))
    (.-hostname js/location)))

(defn- get-proto []
  (if (= (.-protocol js/location) "https:") "wss:" "ws:"))

(defn- get-port []
  (if (exists? js/PORTAL_HOST)
    (js/parseInt (second (str/split js/PORTAL_HOST #":")))
    (when-not (= (.-port js/location) "")
      (js/parseInt (.-port js/location)))))

(defn main! []
  (-> (connect
       {:path     "/ssr"
        :protocol (get-proto)
        :host     (get-host)
        :port     (get-port)
        :session  (get-session)
        :on-message
        (fn [data]
          (if-not (.startsWith ^js/String data "{")
            (on-message {:op "on-render" :html data})
            (-> (.parse js/JSON data)
                (js->clj :keywordize-keys true)
                (on-message))))})
      (.then on-connect)))

(main!)
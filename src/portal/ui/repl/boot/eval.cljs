(ns portal.ui.repl.boot.eval
  (:require [cljs.js :as cljs]
            [clojure.string :as str]
            [portal.ui.boot :as boot]
            [portal.ui.cljs :as repl]
            [portal.ui.load :as load]
            [portal.ui.state :as state]))

(defn eval-str
  ([source]
   (eval-str source {}))
  ([source {:keys [ns line column] :as opts}]
   (let [ns (if (or (symbol? ns) (string? ns))
              (symbol ns)
              'cljs.user)
         line-buffer   (when line
                         (str/join (take (dec line) (repeat "\n"))))
         column-buffer (when column
                         (str/join (take (dec column) (repeat " "))))]
     (js/Promise.
      (fn [resolve reject]
        (cljs/eval-str
         boot/state
         (str line-buffer column-buffer source)
         (or (:name opts)
             (:file opts)
             "<REPL>")
         {:eval cljs/js-eval
          :def-emits-var (:def-emits-var opts true)
          :target :nodejs
          :source-map true
          :context (:context opts :statement)
          :verbose false
          :load (:load opts load/load-fn)
          :ns ns
          :cache-source (:cache-source opts load/cache-source)}
         (fn [{:keys [error] :as result}]
           (when (and (:verbose opts) error) (prn error))
           (if error
             (reject error)
             (do
               (when-let [file (and (= :statement (:context opts)) (:file opts))]
                 (state/dispatch!
                  state/state state/notify
                  {:type :info
                   :icon :file-code
                   :timeout 1250
                   :message (str "File Loaded" " - " file)}))
               (resolve result))))))))))

(defn eval-string [msg]
  (eval-str (:code msg) msg))

(when (exists? js/window)
  (set! (.-eval_cljs js/window) eval-str))

(reset! repl/eval-fn eval-string)
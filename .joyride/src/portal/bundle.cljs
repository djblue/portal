(ns portal.bundle
  (:require ["fs" :as fs]
            ["path" :as path]
            ["vscode" :as vscode]
            [clojure.string :as str]))

(defn- ns->path [ns]
  (-> (str ns)
      (str/replace "." "/")
      (str/replace "-" "_")))

(defn- get-project-root []
  (let [^js uri (-> vscode .-workspace .-workspaceFolders (aget 0) .-uri)
        fs-path (.-fsPath uri)]
    (if-not (undefined? fs-path) fs-path (.-path uri))))

(defn- get-path [ns]
  (let [path (ns->path ns)]
    (some
     (fn [ext]
       (let [path (path/join (get-project-root) "src" (str path ext))]
         (when (fs/existsSync path) path)))
     [".cljs" ".cljc"])))

(defn- get-source [ns]
  (some-> ns get-path (fs/readFileSync "utf8")))

(defn- ns-form->deps [form]
  (or (some
       (fn [form]
         (when (and (coll? form)
                    (= :require (first form)))
           (mapv first (rest form))))
       form)
      []))

(defn- get-deps-1 [ns]
  (some-> ns get-source read-string ns-form->deps))

(defn- get-deps
  ([ns]
   (get-deps {} ns))
  ([deps ns]
   (if (contains? deps ns)
     deps
     (let [d (get-deps-1 ns)]
       (reduce get-deps (assoc deps ns d) d)))))

(defn- sort-deps [deps ns]
  (when-let [d (get deps ns)]
    (concat
     (mapcat (partial sort-deps deps) d)
     [ns])))

(defn- resolve-deps [namespaces]
  (let [deps (reduce get-deps {} namespaces)]
    (->> namespaces
         (mapcat #(sort-deps deps %))
         (distinct))))

(defn- get-extension [extension-name]
  (js/Promise.
   (fn [resolve reject]
     (let [n (atom 16) delay 250]
       (js/setTimeout
        (fn work []
          (try
            (resolve (.-exports (.getExtension vscode/extensions extension-name)))
            (catch :default e
              (if (zero? (swap! n dec))
                (reject (ex-info "Max attempts reached" {} e))
                (js/setTimeout work delay)))))
        delay)))))

(defn setup! []
  (let [scripts (resolve-deps ['portal.api 'portal.console])]
    (-> (get-extension "betterthantomorrow.joyride")
        (.then (fn [joyride]
                 (reduce
                  (fn [chain ns]
                    (-> chain
                        (.then
                         (fn [_]
                           (.runCode joyride (get-source ns))))))
                  (.resolve js/Promise 0)
                  scripts)))
        (.catch (fn [e] (prn e))))))

(defn- get-vendor-code []
  (into []
        (for [ns (resolve-deps ['portal.api 'portal.console])]
          (list 'io/inline
                (-> (get-project-root)
                    (path/join "src")
                    (path/relative (get-path ns)))))))

(comment
  (setup!)
  (get-vendor-code)
  (require '[portal.api :as p])
  (add-tap #'portal.api/submit)
  (p/docs)
  (tap> :hi)
  (p/open {:launcher :vs-code})
  (p/open {:mode :dev :launcher :vs-code}))
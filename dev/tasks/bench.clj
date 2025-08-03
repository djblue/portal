(ns tasks.bench
  (:require
   [portal.api :as p]
   [portal.bench-cson :as bc]
   [tasks.parallel :refer [with-out-data]]
   [tasks.tools :as t]))

(defn bench-cson []
  (mapcat
   (fn [f]
     (some
      #(let [{:keys [tag val]} %]
         (when (= :tap tag) val))
      @f))
   (for [f [#(t/clj "-M:test" "-m" :portal.bench-cson)
            #(t/bb "-m" :portal.bench-cson)
            #(t/cljs "1.10.773" :portal.bench-cson)
            #(t/cljr "-m" :portal.bench-cson)
            #(t/nbb "-m" :portal.bench-cson)
            #(t/lpy :run "-n" :portal.bench-cson)]]
     (future (with-out-data (f))))))

(def windows
  {{:encoding :cson :test :read}
   {:launcher :auto :window-title "cson-read"}
   {:encoding :cson :test :write}
   {:launcher :auto :window-title "cson-write"}
   {:runtime :clj :test :read}
   {:launcher :auto :window-title "clj-read"}
   {:runtime :clj :test :write}
   {:launcher :auto :window-title "clj-write"}
   {:runtime :cljs :test :read}
   {:launcher :auto :window-title "cljs-read"}
   {:runtime :cljs :test :write}
   {:launcher :auto :window-title "cljs-write"}})

(defn charts [data]
  #_(p/inspect (bc/charts data) {:launcher :auto :window-title "cson-all"})
  #_(let [groups (group-by #(select-keys % [:runtime :test]) data)]
      (doseq [[group opts] windows
              :let [sub-data (get groups group)]
              :when sub-data]
        (p/inspect (bc/charts sub-data) opts)))
  (let [groups (group-by #(select-keys % [:encoding :test]) data)]
    (doseq [[group opts] windows
            :let [sub-data (get groups group)]
            :when sub-data]
      (p/inspect (bc/charts sub-data) opts))))

(defn -main
  "Run cson benchmarks and render results in portal"
  []
  (.addShutdownHook
   (Runtime/getRuntime)
   (Thread. (fn [] (p/close))))
  (try
    (charts (bench-cson))
    (catch Exception e
      (p/inspect (Throwable->map e) {:launcher :auto})))
  @(promise))
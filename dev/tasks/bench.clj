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
   {:launcher :auto :window-title "cson-write"}})

(defn charts [data]
  (let [groups (group-by #(select-keys % [:encoding :test]) data)]
    (doseq [[group opts] windows]
      (p/inspect (bc/charts (get groups group groups)) opts))))

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
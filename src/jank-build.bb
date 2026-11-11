(require '[babashka.fs :as fs]
         '[jank.build.util :as util])

#_{:clj-kondo/ignore [:unresolved-symbol]}
(let [{:keys [src-dir]} *input*]
  (util/include-dir src-dir)
  (util/include-dir (fs/path src-dir "portal/include")))

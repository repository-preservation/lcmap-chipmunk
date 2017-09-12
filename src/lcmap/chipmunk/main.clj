(ns lcmap.chipmunk.main
  "App startup related functions and entrypoint."
  (:require [clojure.tools.logging :as log]
            [mount.core]
            [lcmap.chipmunk.config]
            [lcmap.chipmunk.db]
            [lcmap.chipmunk.gdal]
            [lcmap.chipmunk.http]
            [lcmap.chipmunk.jmx]
            [lcmap.chipmunk.layer]
            [lcmap.chipmunk.setup]
            [lcmap.chipmunk.util])
  (:gen-class))


(defn -main
  ""
  [& args]
  (try
    (log/debug "chipmunk init")
    (lcmap.chipmunk.setup/init)
    (lcmap.chipmunk.util/add-shutdown-hook)
    (mount.core/start)
    (catch RuntimeException ex
      (log/errorf "error starting chipmunk: %s" (.getMessage ex))
      (log/fatalf "chipmunk died during startup")
      (System/exit 1))))

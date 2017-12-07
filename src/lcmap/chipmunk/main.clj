(ns lcmap.chipmunk.main
  "Start-up related functions and entry-point."
  (:require [clojure.tools.logging :as log]
            [mount.core]
            [lcmap.chipmunk.config]
            [lcmap.chipmunk.db]
            [lcmap.chipmunk.gdal]
            [lcmap.chipmunk.http]
            [lcmap.chipmunk.jmx]
            [lcmap.chipmunk.chips]
            [lcmap.chipmunk.setup]
            [lcmap.chipmunk.util])
  (:gen-class))


(defn -main
  "This is the entry-point used to start a Chipmunk server.

   Arguments are ignored, use ENV variables or profiles.clj
   to configure the app."
  [& args]
  (try
    ;; This needs to happen before mount states are started
    ;; because they expect keyspaces and tables to exist.
    (log/debug "chipmunk init")
    (lcmap.chipmunk.setup/init)
    ;; A shutdown hook gives us a way to cleanly stop mount
    ;; states.
    (log/debug "chipmunk add shutdown hook")
    (lcmap.chipmunk.util/add-shutdown-hook)
    ;; Remember, only mount states defined that are defined
    ;; in required namepsaces are started.
    (log/debug "chipmunk start")
    (mount.core/start)
    (catch RuntimeException ex
      (log/errorf "error starting chipmunk: %s" (.getMessage ex))
      (log/fatalf "chipmunk died during startup")
      (System/exit 1))))

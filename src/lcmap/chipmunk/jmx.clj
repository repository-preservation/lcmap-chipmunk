(ns lcmap.chipmunk.jmx
  ""
  (:require [clojure.tools.logging :as log]
            [metrics.reporters.jmx :as metrics-jmx]
            [mount.core :as mount]))


(defn start-jmx-reporter []
  (log/debug "start jmx listener")
  (let [it (metrics-jmx/reporter {})]
           (metrics-jmx/start it)
           it))


(mount/defstate jmx-reporter
  :start (start-jmx-reporter))

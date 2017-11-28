(ns user
  "Developer namespace."
  (:require [clojure.edn :as edn]
            [clojure.stacktrace :as stacktrace]
            [clojure.spec.alpha :as spec]
            [clojure.xml :as xml]
            [lcmap.chipmunk.db :as db]
            [lcmap.chipmunk.jmx]
            [lcmap.chipmunk.http]
            [lcmap.chipmunk.registry :as registry]
            [lcmap.chipmunk.inventory :as inventory]
            [lcmap.chipmunk.chips :as chips]
            [lcmap.chipmunk.gdal :as gdal]
            [lcmap.chipmunk.core :as core]
            [lcmap.chipmunk.util :as util]
            [lcmap.chipmunk.ard :as ard]
            [lcmap.chipmunk.setup :as setup]
            [lcmap.chipmunk.config :as config]
            [org.httpkit.client :as http]
            [clj-xpath.core :as xpath]
            [qbits.hayt :as hayt]
            [qbits.alia :as alia]
            [mount.core :as mount]))

;;
;; Starting a REPL will automatically setup and start the system.
;;
(try
  (print "setting up chipmunk instance")
  (setup/init)
  (print "starting mount components")
  (mount/start)
  :ready
  (catch RuntimeException ex
    (print "There was a problem automatically setting up and running chipmunk.")
    (stacktrace/print-cause-trace ex)))


(comment
  "Create some layers and tables for chip data."
  (let [layers (-> "registry.lc08.edn" clojure.java.io/resource slurp edn/read-string)]
    (map registry/add! layers))
  (let [layers (-> "registry.le07.edn" clojure.java.io/resource slurp edn/read-string)]
    (map registry/add! layers))
  (let [layers (-> "registry.lt05.edn" clojure.java.io/resource slurp edn/read-string)]
    (map registry/add! layers))
  (let [layers (-> "registry.lt04.edn" clojure.java.io/resource slurp edn/read-string)]
    (map registry/add! layers))
  (let [layers (-> "registry.aux.edn" clojure.java.io/resource slurp edn/read-string)]
    (map registry/add! layers)))

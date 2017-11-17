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


(comment
  "Ingest some test data."
  (let [base "http://guest:guest@localhost:9080"
        tile "LC08_CU_027009_20130701_20170729_C01_V01"
        path "%s/%s_SR.tar/%s_%s.tif"
        f1 (format path base tile tile "SRB1")
        f2 (format path base tile tile "PIXELQA")
        f3 (format path base tile tile "LINEAGEQA")]
    [(core/ingest f1)
     (core/ingest f2)
     (core/ingest f3)]))


(comment
  (let [url "http://localhost:9080/LC08_CU_027009_20130701_20170729_C01_V01_SR.tar"]
    (ard/ard-metadata url)))


(comment
  (let [url "http://localhost:9080/LC08_CU_027009_20130701_20170729_C01_V01.xml"
        doc (-> url ard/ard-metadata-get xpath/xml->doc)]
    {:product_id (first (xpath/$x:text+ "//product_id" doc))
     :satellite  (first (xpath/$x:text+ "//satellite" doc))
     :instrument (first (xpath/$x:text+ "//instrument" doc))
     :collection (first (xpath/$x:text+ "//level1_collection" doc))
     :version    (first (xpath/$x:text+ "//ard_version" doc))
     :region     (first (xpath/$x:text+ "//region" doc))
     :tile_h     (first (xpath/$x:attrs* "//tile_grid" doc :h))
     :tile_v     (first (xpath/$x:attrs* "//tile_grid" doc :v))
     ;; Scene metadata
     :scene_center_time (first (xpath/$x:text+ "//scene_center_time" doc))
     :acquisition_date  (first (xpath/$x:text+ "//acquisition_date" doc))
     }))

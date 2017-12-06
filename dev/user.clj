(ns user
  "Developer namespace."
  (:require [clojure.edn :as edn]
            [clojure.stacktrace :as stacktrace]
            [lcmap.chipmunk.jmx]
            [lcmap.chipmunk.http]
            [lcmap.chipmunk.db :as db]
            [lcmap.chipmunk.registry :as registry]
            [lcmap.chipmunk.inventory :as inventory]
            [lcmap.chipmunk.chips :as chips]
            [lcmap.chipmunk.gdal :as gdal]
            [lcmap.chipmunk.grid :as grid]
            [lcmap.chipmunk.util :as util]
            [lcmap.chipmunk.ard :as ard]
            [lcmap.chipmunk.setup :as setup]
            [lcmap.chipmunk.config :as config]
            [lcmap.chipmunk.ingest :as ingest]
            [mount.core :as mount])
  (:import [org.joda.time DateTime]))

;;
;; Starting a REPL will automatically setup and start the system.
;;

(try
  (print "setting up chipmunk instance...")
  (setup/init)
  (print "starting mount components...")
  (mount/start)
  (print "...ready!")
  (catch RuntimeException ex
    (print "There was a problem automatically setting up and running chipmunk.")
    (stacktrace/print-cause-trace ex)))


(comment
  "Create some layers and tables for chip data."
  (let [layers (-> "registry.ard.edn" clojure.java.io/resource slurp edn/read-string)]
    (map registry/add! layers))
  (let [layers (-> "registry.aux.edn" clojure.java.io/resource slurp edn/read-string)]
    (map registry/add! layers))
  (let [grids  (-> "grid.conus.edn" clojure.java.io/resource slurp edn/read-string)]
    (map grid/add! grids)))


(comment
  "Ingest sample data."
  (let [ard-url "http://localhost:9080/LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif"
        aux-url "http://localhost:9080/AUX_CU_027009_20000731_20171031_V01.tar/AUX_CU_027009_20000731_20171031_V01_TRENDS.tif"]
    (ingest/save ard-url)
    (ingest/save aux-url)))


(comment
  "Querying the inventory (not getting chips) for source metadata."
  (let [ard-url "http://localhost:9080/LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif"
        aux-url "http://localhost:9080/AUX_CU_027009_20000731_20171031_V01.tar/AUX_CU_027009_20000731_20171031_V01_TRENDS.tif"]
    (inventory/search {:url aux-url})))


(comment
  "Getting some chips"
  (let [query {:x 1484416 :y 1961804 :acquired "2013/2014" :ubid "LC08_SRB1"}]
    (chips/search! query)))


(comment
  "Getting some chip-specs"
  (let [query {:ubid "LC08_SRB1"}]
    (registry/search! query)))


(comment
  "Working with grids, notice the 'off-the-grid' point."
  (let [grid (grid/search "chip")]
    (grid/grid-snap {:x 1526416 :y 1946804} grid))
  (let [grid (grid/search "chip")]
    (grid/proj-snap {:x 1526416 :y 1946804} grid))
  (let [grid (grid/search "chip")]
    (grid/snap {:x 1526416 :y 1946804} grid))
  (let [grid (grid/search "chip")]
    (grid/near {:x 1526416 :y 1946804} grid)))

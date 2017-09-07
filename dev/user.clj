(ns user
  "Developer namespace."
  (:require [lcmap.chipmunk.db :as db]
            [lcmap.chipmunk.jmx]
            [lcmap.chipmunk.http]
            [lcmap.chipmunk.layer :as layer]
            [lcmap.chipmunk.gdal :as gdal]
            [lcmap.chipmunk.core :as core]
            [lcmap.chipmunk.util :as util]
            [lcmap.chipmunk.setup :as setup]
            [mount.core :as mount]))

;;
;; Starting a REPL will automatically setup and start the system.
;;
(try
  (setup/init)
  (mount/start)
  :ready
  (catch RuntimeException ex
    (print "Hmm. There was a problem automatically setting up and running chipmunk.")))


(comment
  "Create some layers and tables for chip data."
  (map layer/create! [:LC08_SRB1 :LC08_PIXELQA :LC08_LINEAGEQA]))


(comment
  "Ingest some test data."
  (let [base "/vsitar/vsicurl/http://guest:guest@localhost:9080/"
        tile "LC08_CU_027009_20130701_20170430_C01_V01"
        path "%s/%s_SR.tar/%s_%s.tif"
        f1 (format path base tile tile "SRB1")
        f2 (format path base tile tile "PIXELQA")
        f3 (format path base tile tile "LINEAGEQA")
        counter (comp count core/chip-seq)]
    [(map counter [f1 f2 f3])
     (core/ingest f1 :LC08_SRB1)
     (core/ingest f2 :LC08_PIXELQA)
     (core/ingest f3 :LC08_LINEAGEQA)]))

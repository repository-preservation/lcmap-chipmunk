(ns lcmap.chipmunk.shared
  (:require [lcmap.chipmunk.setup]
            [lcmap.chipmunk.gdal]))


;; Important:
;;
;; In order for tests to run you need to start the backing
;; services by running `make docker-compose-up`
;;

;; This will configure the keyspace and table for registering
;; layers.
;;
(lcmap.chipmunk.setup/init)

;; GDAL must be initialized manually until state is managed
;; using something like mount.
;;
(lcmap.chipmunk.gdal/init)

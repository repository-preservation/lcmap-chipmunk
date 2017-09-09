(ns lcmap.chipmunk.shared
  (:require [mount.core :as mount]
            [cheshire.core :as json]
            [org.httpkit.client :as http]
            [qbits.alia :as alia]
            [qbits.hayt :as hayt]
            [lcmap.chipmunk.setup]
            [lcmap.chipmunk.config :as config]
            [lcmap.chipmunk.core :as core]
            [lcmap.chipmunk.gdal]
            [lcmap.chipmunk.http]
            [lcmap.chipmunk.db]
            [lcmap.chipmunk.layer :as layer]
            [lcmap.chipmunk.inventory :as inventory]
            [lcmap.chipmunk.registry :as registry]))


;; Important:
;;
;; In order for tests to run you need to start the backing
;; services by running `make docker-compose-up`
;;

;; This will configure the keyspace and table for registering
;; layers. Setup maintains (opens and closes) connections to
;; the DB on its own, so mount doesn't need to be started or
;; stopped.
;;
(lcmap.chipmunk.setup/init)

;; GDAL must be initialized manually until state is managed
;; using something like mount.
;;
(lcmap.chipmunk.gdal/init)


;; This is a path to some real data.
;;
(def path-to-data "http://guest:guest@localhost:9080/LC08_CU_027009_20130701_20170430_C01_V01_SR.tar/LC08_CU_027009_20130701_20170430_C01_V01_SRB2.tif")

;;
;; Fixtures
;;


(defn mount-fixture [f]
  (mount/start)
  (f)
  (mount/stop))


(defn layer-fixture [f]
  (registry/add! "test_layer")
  (f)
  (registry/remove! "test_layer"))


(defn layer-data-fixture [f]
  (core/ingest "test_layer" "test_source" path-to-data)
  (f))


;;
;; Utilities
;;

(defn app-url
  "Produce a URL for path to locally running HTTP server. Used for
   integration testing."
  [path]
  (let [port (-> config/config :http-port Integer/parseInt)]
    (str (java.net.URL. (java.net.URL. "http" "localhost" port "/") path))))


(defn try-decode
  "Attempt to decode body as JSON, but leave it be otherwise."
  [body]
  (try
    (json/decode body keyword)
    (catch java.lang.RuntimeException ex
      body)))


(defn go-fish
  "Helper function for integration tests."
  [opts]
  (let [port (config/config :http-port)
        base (format "http://localhost:%s/" port)]
    (-> opts
        (update-in [:url] (fn [url] (app-url url)))
        (assoc-in  [:headers "Content-Type"] "application/json")
        (update-in [:body] json/encode)
        (http/request)
        (deref)
        (update-in [:body] try-decode))))

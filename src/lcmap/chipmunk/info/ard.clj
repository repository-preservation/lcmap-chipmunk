(ns lcmap.chipmunk.info.ard
  "Provides functions for retrieving and parsing ARD XML metadata."
  (:require [clj-xpath.core :as xpath]
            [org.httpkit.client :as http])
  (:import [org.joda.time DateTime]))


(defn locate
  "Given a URL to an ARD source, produce URL to its XML metadata."
  [url]
  (let [pattern #"_(SR|TA|QA|BT)\.[(tar|tif)](.*)$"]
    (if (some? (re-find pattern url))
      (clojure.string/replace url pattern ".xml")
      nil)))


(defn fetch
  "Given a URL to XML metadata, retrieve the contents."
  [url]
  (let [resp @(http/get url)]
    (if (<= 200 (resp :status) 299)
      (resp :body))))


(defn parse
  "Given an XML string, produce a map of relevant metadata."
  [xml]
  (try
    (let [doc (xpath/xml->doc xml)]
      {:product_id (first (xpath/$x:text+ "//product_id" doc))
       :satellite  (first (xpath/$x:text+ "//satellite" doc))
       :instrument (first (xpath/$x:text+ "//instrument" doc))
       :collection (first (xpath/$x:text+ "//level1_collection" doc))
       :version    (first (xpath/$x:text+ "//ard_version" doc))
       :region     (first (xpath/$x:text+ "//region" doc))
       :tile_h     (first (xpath/$x:attrs* "//tile_grid" doc :h))
       :tile_v     (first (xpath/$x:attrs* "//tile_grid" doc :v))
       :acquired   (first (xpath/$x:text+ "//acquisition_date" doc))})
    (catch com.sun.org.apache.xerces.internal.impl.io.MalformedByteSequenceException ex
      (throw (ex-info "could not parse ARD XML metadata" {})))
    (catch java.lang.RuntimeException ex
      (throw (ex-info "could not retrieve expected values from ARD XML metadata" {})))))


(defn prep-time
  "Helper function to create a DateTime from a Date and a Time."
  [info]
  (.toDate (DateTime/parse (:acquired info))))


(defn prep-tile
  "Combine two separate values for a tile into one."
  [info]
  (str (:tile_h info) (:tile_v info)))


(defn prep-info
  "Update or add derived values to the info map."
  [info]
  (-> info
      (assoc :acquired (prep-time info))
      (assoc :tile (prep-tile info))))


(defn get-info-for
  "Given a URL to a source, retrieve related metadata.

   This is distinct from the embedded metadata contained in a GDAL
   dataset; it is for reading USGS ARD metadata."
  [url]
  (some-> url locate fetch parse prep-info))

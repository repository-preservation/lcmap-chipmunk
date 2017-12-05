(ns lcmap.chipmunk.ard
  "Provides functions for retrieving and parsing ARD XML metadata."
  (:require [clj-xpath.core :as xpath]
            [org.httpkit.client :as http])
  (:import [org.joda.time DateTime]))


;; ## Overview
;;
;; Landsat ARD XML metadata contains information about the contents
;; of an archive, a collection of related bands. It provides details
;; about the original scenes and produced tiles.
;;


;; ### Locate
;;
;; This function relies on a convention to derive a URL to the XML
;; file: it replaces the last portion of the archive's filename
;; with '.xml' as shown below.
;;
;; If this pattern cannot be found, a nil value is returned.
;;
;; http://host/LC08_...C01_V01_SR.tar/LC08_..._C01_V01_SRB1.tif
;; http://host/LC08_...C01_V01.xml
;;

(defn locate
  "Given a URL to an ARD source, produce URL to its XML metadata."
  [url]
  (let [pattern #"_(SR|TA|QA|BT)\.[(tar|tif)](.*)$"]
    (if (some? (re-find pattern url))
      (clojure.string/replace url pattern ".xml")
      nil)))


;; ### Fetch
;;
;; Once a URL is derived, the fetch function attempts to obtain
;; the contents of the XML file. A status check is performed to
;; avoid attempting to process a non-XML body; a URL to a non-
;; existent file may return plain text. A more sophisticated check
;; could be performed, but this works well-enough.
;;

(defn fetch
  "Given a URL to XML metadata, retrieve the contents."
  [url]
  (let [resp @(http/get url)]
    (if (<= 200 (resp :status) 299)
      (resp :body))))


;; ### Parse
;;
;; The parse function retrieves a relevant subset of the info from
;; the XML. If additional data is required, this is the function to
;; modify.
;;

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

;; Preparing Values
;;
;; After retrieving values, a tile ID is derived and the datetime
;; is converted into an object.
;;

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


;; ## Getting Info
;;

(defn get-info-for
  "Given a URL to a source, retrieve related metadata.

   This is distinct from the embedded metadata contained in a GDAL
   dataset; it is for reading USGS ARD metadata."
  [url]
  (some-> url locate fetch parse prep-info))

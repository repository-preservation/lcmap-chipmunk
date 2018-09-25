(ns lcmap.chipmunk.ingest
  "Functions used to inspect, extract, and persist source data."
  (:require [lcmap.chipmunk.util :as util]
            [lcmap.chipmunk.gdal :as gdal]
            [lcmap.chipmunk.chips :as chips]
            [lcmap.chipmunk.inventory :as inventory]
            [lcmap.chipmunk.ard  :as ard]
            [org.httpkit.client :as http]
            [digest :as digest])
  (:import [java.net URI]
           [java.io File]))

(set! *warn-on-reflection* true)

;; # Overview
;;
;; Functions are organized in one of three groups:
;;
;; 1. Inspect: Get information about the source.
;; 2. Extract: Get raster data from the source.
;; 3. Persist: Put the source info and raw data in the DB.
;;


;; ## Inspect
;;
;; The inspection functions get and deriver different types of information
;; about a source. This includes info from the file name, GDAL metadata,
;; the HTTP response of the resource, and ARD XML metadata.
;;


;; ### Path Info
;;
;; Thanks to file name conventions, it is possible to obtain useful info
;; about the source. For example, the mission, region, acquisition date,
;; and band name.
;;
;; The `path-info` function uses `*source-pattern*` and `*source-groups*`
;; to build an info map from a URL. If a URL does not match this pattern
;; then it shouldn't be ingested. Using rebindable vars lays groundwork
;; for a configurable file-name pattern.
;;
;; The given pattern has an optional 'collection' group -- auxiliary data
;; deviates slightly from the Landsat ARD naming convention.
;;

(def ^:dynamic *source-pattern*
  #"(?x)
    (?<archive>.*)/
    (?<id>
      (?<mission>.+)_
      (?<region>.{2})_
      (?<tile>[0-9]{6})_
      (?<acquired>[0-9]{8})_
      (?<produced>[0-9]{8})_
      (C(?<collection>[0-9]{2})_)? # intentionally optional (aux data)
      (V(?<version>[0-9]{2})_)
      (?<band>[A-Z0-9]+)
      (?<extension>.*))")


;; The group names are provided as a set because the alternative is to
;; use the slower Java Reflection API to dig these properties out of the
;; pattern. However, the implementation details are subject to change
;; between JVMs. Subtle and nuanced failures are to be avoided!
;;

(def ^:dynamic *source-groups*
  #{:archive
    :mission
    :region
    :tile
    :acquired
    :produced
    :collection
    :version
    :band
    :extension
    :id})


(defn path-info
  "Build an info map from the given URL."
  [url]
  (if-let [result (util/re-mapper *source-pattern* *source-groups* url)]
    result
    (throw (ex-info "URL does not match expected pattern for ARD source data." {:url url}))))


;; GDAL information is easy to obtain. The driver is capable of
;; performing ranged HTTP requests to retrieve portions of data
;; without retrieving the file in it's entirety.
;;

(defn gdal-info
  "Build an info map from the source's GDAL metadata."
  [url]
  (gdal/dataset-info url))


;; Obtaining information about the HTTP resource is complicated
;; by the way ARD is structured. Because a GeoTIFF is contained
;; within an archive, we can't just perform a head request for
;; the given URL to get the file size, last modification date, or
;; an etag.
;;
;; Much like ARD XML metadata, a different URL must be derived,
;; one that references the containing tar file, in order to get
;; useful info.
;;
;; Remember, if the values of the GeoTIFF change, then the values
;; of the HTTP head request for the containing archive will change
;; too. However, the archive may be changed in a way that does not
;; change the value of the GeoTIFF. Use this info as a potential
;; indicator that a change has occurred, not an absolute one.
;;

(defn http-info
  "Build an info map from an HTTP HEAD response of the parent resource."
  [url]
  (if-let [info (path-info url) ]
    (-> (:archive info)
      (http/head)
      (deref)
      (dissoc :body))))


;; The ARD XML metadata format is complex enough that its
;; parsing is defined in a separate namespace.
;;

(defn meta-info
  "Build an info map from the related ARD XML metadata for the file."
  [url]
  (ard/get-info-for url))


;; By convention, the ID of a source is the filename. This
;; function invokes `path-info` to safe-guard against calls
;; with a URL that do not conform to expectations.
;;

(defn derive-name
  "Produce a unique ID (name) for the URL."
  [url]
  (if-let [info (path-info url)]
    (:id info)))


;; By convention, the name of the layer is the value of the
;; <mission> and <band> in the URL's path.
;;

(defn derive-ubid
  "Find the layer's name (ubid) for data at URL."
  [url]
  (if-let [info (path-info url)]
    (format "%s_%s" (:mission info) (:band info))))


(defn inspect
  "Invoke all `*-info` functions, and derive the source name and layer ubid."
  [url]
  (let [path-data (path-info url)
        http-data (http-info url)
        gdal-data (gdal-info url)
        meta-data (meta-info url)]
    {:path path-data
     :http http-data
     :gdal gdal-data
     :meta meta-data
     :url  url
     :ubid (derive-ubid url)
     :name (derive-name url)
     :tile (:tile path-data)
     :acquired (util/instantize (or (:acquired meta-data)
                                    (:acquired path-data)))}))


;; ## Extract
;;
;; Read raw data from a source as chips, associating relevant info with each.
;;

(defn +hash
  "Produce and associate and MD5 checksum of the chip's data."
  [chip]
  (assoc chip :hash (-> chip :data util/byte-buffer-copy digest/md5)))


(defn +point
  "Calculate and associate the chip's projection coordinate point."
  [chip dataset]
  (let [matrix (gdal/get-geo-transform dataset)
        [x y]  (gdal/geo-transform matrix (chip :raster-x) (chip :raster-y))]
    (assoc chip :x (long x) :y (long y))))

(defn +info
  "Associate relevant info with chip."
  [chip info]
  (assoc chip
         :layer    (info :ubid)
         :source   (info :name)
         :acquired (info :acquired)))


;; Producing chips requires some basic geometry related parameters that
;; describes how a raster is traversed for data. These are raster grid
;; units (i.e. pixels), not projection grid units (i.e. meters).
;;

(def ^:dynamic *opts* {:xstart 0 :ystart 0 :xstop 5000 :ystop 5000 :xstep 100 :ystep 100})

(defn extract
  "Produce a sequence of chips, along with a checksum of the data and projection coordinate."
  ([url info]
   (gdal/with-data [dataset (gdal/open url)]
     (let [band (gdal/band dataset)
           data (gdal/raster-seq band *opts*)
           tf   (comp (map #(+hash %))
                      (map #(+info % info))
                      (map #(+point % dataset)))]
       (into [] tf data)))))


;; ### Summary
;;
;; A summary provides insight about an ingested source of data: what was ingested
;; and how it went. The summary, stored in an inventory, is indexed by the derived
;; ID of the source (from the URL), the layer, and the tile.
;;
;; However, all of the metadata that was obtained in preparation for saving actual
;; chips is preserved. This helps us avoid wish-we-had-it types of situations.
;;
;; Further, everything about a chip is stored except for the actual chip data itself.
;;


(defn summarize
  "Combine entirety of info, but omit raw chip data, ready to save to inventory."
  [chips info]
  {:source (info :name)
   :layer  (info :ubid)
   :url    (info :url)
   :tile   (info :tile)
   :extra  info
   :chips  (map #(select-keys % [:x :y :acquired :hash]) chips)})


;; ## Save
;;
;; 1. inspect: Get information about the source from the URL, from ARD XML
;;             metadata, from GDAL metadata, and from the 'resource' that
;;             contains the raster (i.e. the tar file).
;; 2. extract: Produce a sequence of chips, augmented with info obtained from
;;             step one. The raster has no intrinsic values that provide a
;;             unique identity or acquisition datetime.
;; 3. persist: Save every chip. Then, save a summary that contains info about
;;             the source and everything except the raw chip data. This helps
;;             determine what is held in the system without directly querying
;;             chips.

(defn save
  "Save data at URL. Persist chips (to a layer) and a summary to the inventory."
  ([url]
   (let [info (inspect url)
         data (extract url info)]
     (-> data (chips/save)
              (summarize info)
              (inventory/save)))))

(ns lcmap.chipmunk.core
  "Chipmunk turns USGS ARD files into HTTP accessible time-series data.

   These transformations, although particular to the USGS ARD definition,
   are not specific to the underlying persistence mechanism. The intent
   is to produce a sequence of data with all additional context while
   keeping persistence logic separate and isolated.

   Chips have the following properties:

   ^java.nio.ByteBuffer :data: A direct byte buffer that contains the
       original raster data as-is.
   ^Integer :raster-x: The upper-left raster coordinate of the data,
       this is also referred to as the 'point' in line-point terms.
   ^Integer :raster-y: The upper-left raster coordinate of the data,
       this is also referred to as the 'line' in line-point terms.
   ^Integer :raster-hash: A value derived from the contents of the
       data buffer, useful for detecting chips that are entirely fill.
   ^Double :x: The upper-left coordinate of data in coordinate space,
       it is not guaranteed to be an integer.
   ^Double :y: The upper-left coordinate of data in coordinate space,
       it is not guaranteed to be an integer.
   ^String :source: A unique identifier for where the original raster,
       used to keep track of where data was obtained.
   ^String :mission: A four alphanumeric ID for the satellite that collected
       the data.
   ^String :area: A two letter ID for the projection (e.g. CU, AK, HI) that
       also implies an extent and tile identifier.
   ^String :tile: A six number code that implies the ARD 'tile' to which this
       chip belongs (e.g. 003009).
   ^String :produced: An ISO8601 date used to determine when the tile
       was created, do not mistake this with the acquired property!
   ^String :acquired: An ISO8601 data used to detrmine the day the data
       was originally obtained.
   ^String :version: An identifier for the version of an algorithm used to
       do low-level processing of data.
   ^String :collection: An identifier for the version of an algorithm used to
       do high-level processing of data.
   ^String :band: An identifier for the 'layer' of data. The band is *not*
       intrinsically specific to a mission, but can be combined with other
       properties to derive a unique projection-mission-band identifier.
   ^String :format: The file format used to represent data (e.g. geotiff,
       netCDF, etc...)
   "
  (:require [clojure.tools.logging :as log]
            [digest :as digest]
            [lcmap.chipmunk.gdal :as gdal]
            [lcmap.chipmunk.layer :as layer]
            [lcmap.chipmunk.inventory :as inventory]
            [lcmap.chipmunk.registry :as registry]))


(defn byte-buffer-copy
  "Create a byte-array with copy of buffer's contents.

   ^java.nio.ByteBuffer :byte-buffer:
  "
  [byte-buffer]
  (let [size (.capacity byte-buffer)
        copy (byte-array size)]
    (.get byte-buffer copy)
    (.rewind byte-buffer)
    copy))


(defn as-byte-array
  "Transform function, adds hash-code for data buffer to chip.

   ^Map :chip:
  "
  [chip]
  (assoc chip :data (-> chip :data byte-buffer-copy)))


(defn check
  "Transform function, adds hash-code for data buffer to chip.

   ^Map :chip:
  "
  [chip]
  (assoc chip :hash (-> chip :data byte-buffer-copy digest/md5)))


(defn point
  "Transform function, adds coordinate-system point from
   raster pixel/line (x/y) to chip.

   ^Map :chip:
   ^String :path:
  "
  [chip dataset]
  (let [matrix (.GetGeoTransform dataset)
        [x y]  (gdal/geo-transform matrix (chip :raster-x) (chip :raster-y))]
    (merge chip {:x (long x) :y (long y)})))


(defn parse-date
  "Turn a basic (undelimited) IS08601 date into one with delimiters.

   ^String :date: ISO8601 basic date."
  [date]
  (->> (str date)
       (re-matches #"([0-9]{4})([0-9]{2})([0-9]{2})")
       (rest)
       (clojure.string/join "-")))


(defn derive-info
  "Derive additional info from path to source.

   ^String :path:
   ^Map :more:
  "
  [path more]
  ;; !!!
  ;; Please Note: This is specific to USGS:ARD naming conventions.
  ;; !!!
  (let [keys [:source :mission :area :tile :acquired :produced :collection :version :band :format]
        vals (re-find #"([A-Z0-9]{4})_(.{2})_(.{6})_(.{8})_(.{8})_(C.{2})_(V.{2})_([A-Z0-9]+)\.(tif)$" path)]
    (-> (zipmap keys vals)
        (update :acquired parse-date)
        (update :produced parse-date)
        (merge more))))


(defn chip-seq
  "Produce a seq of chips with added context.

   ^String :path: URL to raster data; works with GDAL's VSI feature,
     but must only refer to a single raster image.
   ^Map :info: Additional source metadata.

   return: Sequence of chips with added context.
  "
  ([url info opts]
   ;; This is intentionally eager, not lazy.
   (gdal/with-data [dataset (gdal/open url)]
     (let [band (gdal/band dataset)
           source   (:source info)
           layer    (:layer info)
           acquired (:acquired info)]
       (into []
             ;; transform function
             (comp (map #(assoc % :source source))
                   (map #(assoc % :acquired acquired))
                   (map #(assoc % :layer layer))
                   (map #(point % dataset))
                   (map #(check %)))
             ;; collection of chips
             (gdal/raster-seq band opts)))))
  ([path info]
   (chip-seq path info {:xstart 0 :ystart 0 :xstop 5000 :ystop 5000 :xstep 100 :ystep 100})))


(defn summarize
  ""
  [chips info]
  (assoc info :chips (map #(select-keys % [:x :y :hash]) chips)))


(defn compatible?
  ""
  [layer info]
  (let [pattern (-> layer (get :re_pattern "") re-pattern)
        path (info :path)]
    (seq? (re-seq pattern path))))


(defn verify
  "Throw an exception if the layer and data at URL are not compatible"
  [layer-id source-id url]
  (let [layer (registry/lookup! layer-id)
        info  (gdal/dataset-info url)]
    #_(log/debugf "verify layer: %s" layer)
    #_(log/debugf "verify gdal info: %s" info)
    (if-not (some? layer)
      (throw (ex-info (format "layer does not exist '%s'" layer-id) {})))
    (if-not (some? info)
      (throw (ex-info (format "source does not exist '%s'" url) {})))
    (if-not (compatible? layer info)
      (throw (ex-info (format "layer '%s' not intended for source '%s'" layer-id url) {})))
    true))


(defn ingest
  "Save data at url; adds chips to layer and source info to inventory.
  "
  [layer-id source-id url]
  (try
    (let [info (derive-info url {:source source-id :layer layer-id :url url})]
      (-> (chip-seq url info)
          (layer/save!)
          (summarize info)
          (inventory/save!)))
    (catch java.lang.NullPointerException cause
      (let [msg "GDAL couldn't open source data"]
        (log/errorf "ingest failed: %s" msg)
        (throw (ex-info msg {} cause))))
    (catch com.datastax.driver.core.exceptions.InvalidQueryException cause
      (let [msg "DB statement invalid."]
        (log/errorf "ingest failed: %s" msg)
        (throw (ex-info msg {} cause))))))

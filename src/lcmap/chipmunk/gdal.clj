(ns lcmap.chipmunk.gdal
  "Minimal GDAL Java utility functions."
  (:require [clojure.tools.logging :as log]
            [mount.core :as mount]
            [lcmap.chipmunk.util :as util])
  (:import [org.gdal.gdal gdal]
           [org.gdal.gdalconst gdalconst]))

(set! *warn-on-reflection* true)

;; ## Init
;;
;; This makes it easier to use Java GDAL libraries without
;; having to set environment variables. These are typical
;; install locations of GDAL libs on CentOS and Ubuntu.
;;
;; Before GDAL can open files, drivers must be registered.
;; Selective registration is more tedious and error prone,
;; so we just register all drivers.
;;
;; If anything goes wrong, a helpful string is printed to
;; stdout (not a log file).
;;

(defn init
  "Initialize GDAL drivers."
  []
  (try
    (util/amend-usr-path ["/usr/lib/java/gdal" "/usr/lib/jni"])
    (gdal/AllRegister)
    (catch RuntimeException e
      (binding [*out* *err*]
        (println (str "Could not update paths to native libraries. "
                      "You may need to set LD_LIBRARY_PATH to the "
                      "directory containing libgdaljni.so"))))
    (finally
      (import org.gdal.gdal.gdal))))


;; ## State
;;
;; A mount state is defined so that GDAL is initialized like
;; everything else (DB connections, HTTP listeners, etc...)
;;

(mount/defstate gdal-init
  :start (init))


;; ## VSI Prefixes
;;
;; This function adds transparent support for URLs that appear
;; to need `vsitar` and `vsicurl` prefixes.
;;
;; GDAL provides a variety of ways to access data that is not
;; on a local file-system. When Chipmunk ingests data, it is
;; preferable to avoid downloading, unarchiving, uncompressing,
;; and removing temporary files.
;;
;;

(defn add-vsi-prefix
  "Make an ordinary URL a GDAL path with VSI prefixes."
  [url]
  (cond
    (re-seq #"http.+\.tar.+\.tif" url) (str "/vsitar/vsicurl/" url)
    (re-seq #"http.+\.tif" url) (str "/vsicurl/" url)
    :else url))


(defn open
  "Open dataset at path."
  [path]
  (log/tracef "GDAL open dataset '%s'" path)
  (gdal/Open (add-vsi-prefix path)))


(defn close
  "Free resources associated with an open dataset."
  [^org.gdal.gdal.Dataset ds]
  (log/tracef "GDAL close dataset")
  (if ds (.delete ds)))


;; ## Java Interop. Functions
;;
;; These functions provide a layer on-top of the native Java
;; methods provided by the GDAL library. A variety of functions
;; work directly with arrays; these attempt to make things
;; easier to work with.
;;

(defn geo-transform
  "Transform raster pixel and line to coordinate system (x,y)"
  [matrix raster-x raster-y]
  (let [gx (double-array 1)
        gy (double-array 1)]
    (gdal/ApplyGeoTransform matrix raster-x raster-y gx gy)
    [(first gx) (first gy)]))


(defn get-projection
  "Get a dataset's projection as well known text."
  [^org.gdal.gdal.Dataset ds]
  (.GetProjection ds))


(defn get-raster-count
  "Get the number of raster contained in the dataset."
  [^org.gdal.gdal.Dataset ds]
  (.GetRasterCount ds))


(defn get-raster-x-size
  "Get the dataset's number of pixels in the raster's x dimension."
  [^org.gdal.gdal.Dataset ds]
  (.GetRasterXSize ds))


(defn get-raster-y-size
  "Get the dataset's number of pixels in the raster's y dimension."
  [^org.gdal.gdal.Dataset ds]
  (.GetRasterYSize ds))


(defn get-geo-transform
  "Get the dataset's affine transform matrix as an array."
  [^org.gdal.gdal.Dataset ds]
  (.GetGeoTransform ds))


(defn get-geo-transform-vec
  "Get the dataset's affine transform matrix as a vector."
  [^org.gdal.gdal.Dataset ds]
  (vec (get-geo-transform ds)))


(defn get-metadata 
  "Get the dataset's metadata as a map."
  [^org.gdal.gdal.Dataset ds]
  (.GetMetadata_Dict ds))


(defn band
  "Get the dataset's band indicated by ix, a one-based index."
  ([^org.gdal.gdal.Dataset ds ix]
   (.GetRasterBand ds ix))
  ([^org.gdal.gdal.Dataset ds]
   (.GetRasterBand ds 1)))


(defn get-data-type
  "GDAL data type of the band."
  [^org.gdal.gdal.Band band]
  (.getDataType band))


(defn read-raster-direct
  "Read a region of raster data and return a DirectByteBuffer"
  ([^org.gdal.gdal.Band band ^Integer xoff ^Integer yoff ^Integer xsize ^Integer ysize]
   (let [^Integer buf-type (get-data-type band)]
     (.ReadRaster_Direct band xoff yoff xsize ysize buf-type))))


;; ## Convenience Functions
;;
;; These function build on the basic GDAL library functions,
;; you won't find anything equivalent in the Java library.
;;

(defn raster-seq
  "Provide a seq of raster data with additional context."
  [band {:keys [xstart ystart xstop ystop xstep ystep]}]
  (let [reader #(read-raster-direct band %1 %2 xstep ystep)]
    (for [x (range xstart xstop xstep)
          y (range ystart ystop ystep)]
      {:raster-x x :raster-y y :data (reader x y)})))


(defmacro with-data
  "Use an already open dataset and then close it."
  [bindings & body]
  `(let ~(subvec bindings 0 2)
     (try
       (do ~@body)
       (finally
         (close ~(bindings 0))))))


(defn dataset-info
  "Get some information about the dataset at path, more convenient
   than building things by hand."
  [path]
  (with-data [data (open path)]
    (if data
      {:path          path
       :metadata      (get-metadata data)
       :projection    (get-projection data)
       :raster-x-size (get-raster-x-size data)
       :raster-y-size (get-raster-y-size data)
       :raster-count  (get-raster-count data)
       :geo-transform (get-geo-transform-vec data)}
      (throw (ex-info (format "could not retrive GDAL info for %s" path) {:path path})))))

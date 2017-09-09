(ns lcmap.chipmunk.gdal
  "Minimal GDAL Java utility functions."
  (:require [clojure.tools.logging :as log]
            [mount.core :as mount]
            [lcmap.chipmunk.util :as util])
  (:import [org.gdal.gdal gdal]
           [org.gdal.gdalconst gdalconst]))


(defn init
  "Initialize GDAL drivers."
  []
  (try
    ;; This makes it easier to use Java GDAL libraries without
    ;; having to set environment variables. These are typical
    ;; install locations of GDAL libs on CentOS and Ubuntu.
    (util/amend-usr-path ["/usr/lib/java/gdal" "/usr/lib/jni"])
    ;; Before GDAL can open files, drivers must be registered.
    ;; Selective registration is more tedious and error prone,
    ;; so we just register all drivers.
    (gdal/AllRegister)
    ;; If something does go wrong a helpful error is displayed.
    (catch RuntimeException e
      (binding [*out* *err*]
        (println (str "Could not update paths to native libraries. "
                      "You may need to set LD_LIBRARY_PATH to the "
                      "directory containing libgdaljni.so"))))
    (finally
      (import org.gdal.gdal.gdal))))


(mount/defstate gdal-init
  :start (init))


(defn open
  "Open dataset at path."
  [path]
  (log/debugf "GDAL open dataset")
  (gdal/Open path))


(defn close
  "Free resources associated with an open dataset."
  [ds]
  (log/debugf "GDAL close dataset")
  (.delete ds))


(defmacro with-data
  "Use an already open dataset and then close it."
  [bindings & body]
  `(let ~(subvec bindings 0 2)
     (try
       (do ~@body)
       (finally
         (close ~(bindings 0))))))


(defn geo-transform
  "Transform raster pixel and line to coordinate system (x,y)"
  [matrix raster-x raster-y]
  (let [gx (double-array 1)
        gy (double-array 1)]
    (gdal/ApplyGeoTransform matrix raster-x raster-y gx gy)
    [(first gx) (first gy)]))


(defn get-projection [ds]
  (.GetProjection ds))


(defn get-raster-count [ds]
  (.GetRasterCount ds))


(defn get-geo-transform [ds]
  (.GetGeoTransform ds))


(defn band
  ([ds ix]
   (.GetRasterBand ds ix))
  ([ds]
   (.GetRasterBand ds 1)))


(defn get-data-type
  "GDAL data type of the band."
  [band]
  (.getDataType band))


(defn read-raster-direct
  "Read a region of raster data and return a DirectByteBuffer"
  ([band xoff yoff xsize ysize]
   (let [buf-type (get-data-type band)]
     (.ReadRaster_Direct band xoff yoff xsize ysize buf-type))))


(defn raster-seq
  "Provide a seq of raster data with additional context."
  [band {:keys [xstart ystart xstop ystop xstep ystep]}]
  (let [reader #(read-raster-direct band %1 %2 xstep ystep)]
    (for [x (range xstart xstop xstep)
          y (range ystart ystop ystep)]
      {:raster-x x :raster-y y :data (reader x y)})))

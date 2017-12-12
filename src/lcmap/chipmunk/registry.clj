(ns lcmap.chipmunk.registry
  "Functions for managing layers. These functions will add or remove
   a row to the layer registry and create or drop a table to store
   the the layer's raster data. "
  (:require [clojure.tools.logging :as log]
            [clojure.set :as s]
            [qbits.alia :as alia]
            [qbits.hayt :as hayt]
            [camel-snake-kebab.core :as csk]
            [lcmap.chipmunk.db :as db]))


;; ## Overview
;;
;; The registry contains defintions for the collection of layers
;; a Chipmunk instance provides. It also provides behavior for
;; adding (and removing) the table used by a layer to store data.
;;

;; ### Layer Data Tables
;;
;; When a layer is added to the registry, a corresponding table
;; used to store the raw raster data (chips) is also created. The
;; default columns for these tables are:
;;

(def default-columns {:primary-key [[:x :y], :acquired]
                      :x :bigint
                      :y :bigint
                      :acquired :timestamp
                      :data :blob
                      :hash :text
                      :source :text})


;; These are the options for creating a table for chips, the leveled
;; compaction strategy is optimal for storing time-series data that
;; is potentially saved "out-of-sequence".
;;

(def default-options {:compression {"sstable_compression" "LZ4Compressor"}
                      :compaction  {"class" "LeveledCompactionStrategy"}})

;; ## UBID
;;
;; It is necessary to ensure different values for table names are all
;; converted to a consistent form. This function converts value into
;; a :snake_case_keyword, e.g. 'LC08 SRB1' => :lc08_srb1
;;

(defn canonical-layer-name
  "Convert layer name to lower-case keyword"
  [ubid]
  ;; separate on non-alphanumeric characters
  (try
    (csk/->SCREAMING_SNAKE_CASE ubid :separator #"[\W]+")
    (catch RuntimeException cause
      (let [msg (format "could generate the layer's table name for '%s'" ubid)]
        (log/warn msg)
        (throw (ex-info msg {:ubid ubid} cause))))))


(defn create-layer-table
  "Create table to store layer's chips."
  [{ubid :ubid :as layer}]
  (hayt/create-table (keyword ubid)
                     (hayt/if-exists false)
                     (hayt/column-definitions default-columns)
                     (hayt/with default-options)))


(defn insert-layer-row
  "Add layer to registry."
  [layer]
  (->> (update layer :tags set)
       (hayt/values)
       (hayt/insert :registry)))


(defn add!
  "Add a layer to the registry."
  [layer]
  (try
    (let [layer (update layer :ubid canonical-layer-name)]
      (alia/execute db/db-session (create-layer-table layer))
      (alia/execute db/db-session (insert-layer-row layer))
      layer)
    (catch java.lang.RuntimeException ex
      (let [msg (format "could not create layer %s: %s" layer (.getMessage ex))]
        (throw (ex-info msg {} (.getCause ex)))))))


(defn drop-layer-table
  "Remove layer's corresponding row and table given by UBID."
  [ubid]
  (hayt/drop-table (keyword ubid)))


(defn delete-layer-row
  "A function to delete a layer from the registry given by UBID."
  [ubid]
  (hayt/delete :registry (hayt/where {:ubid (name ubid)})))


(defn remove!
  "Remove layer's corresponding row and table given by UBID."
  [ubid]
  (try
    (alia/execute db/db-session (drop-layer-table ubid))
    (alia/execute db/db-session (delete-layer-row ubid))
    true
    (catch java.lang.RuntimeException cause
      (let [msg (format "could not remove layer '%s'" ubid)]
        (log/error msg)
        false))))


(defn lookup
  "Find layer by UBID."
  [ubid]
  (hayt/select :registry
               (hayt/where {:ubid (canonical-layer-name ubid)})))


(defn lookup!
  "Find layer by UBID."
  [ubid]
  (->> (lookup ubid)
       (alia/execute db/db-session)
       (first)))


(defn all
  "A query to get all layers from the registry."
  []
  (hayt/select :registry))


(defn all!
  "Get all layers from the registry."
  []
  (sort-by :ubid (alia/execute db/db-session (all))))


(defn search!
  "Get layers that have all the given tags."
  ;;
  ;; TODO: case-insensitive comparison?
  ;;
  [params]
  (let [tagset (-> params :tags vector flatten set)
        layers (all!)]
    (if (params :tags)
      (filter #(clojure.set/subset? tagset (% :tags)) layers)
      layers)))

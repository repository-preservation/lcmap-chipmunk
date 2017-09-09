(ns lcmap.chipmunk.registry
  "Functions for managing layers. These functions will add or remove
   a row to the layer registry and create or drop a table to store
   the the layer's raster data. "
  (:require [clojure.tools.logging :as log]
            [qbits.alia :as alia]
            [qbits.hayt :as hayt]
            [lcmap.chipmunk.db :as db]))


;; These are the default columns for a table that contains chips;
;; it is used when creating a layer.
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


(defn create-layer-table
  "Create table to store layer's chips."
  [layer-name]
  (hayt/create-table (keyword layer-name)
                     (hayt/if-exists false)
                     (hayt/column-definitions default-columns)
                     (hayt/with default-options)))


(defn insert-layer-row
  "Add layer to registry."
  [layer-name]
  (hayt/insert :registry (hayt/values {:name layer-name})))


(defn add!
  "Add a layer to the registry."
  [layer-name]
  (alia/execute db/db-session (create-layer-table layer-name))
  (alia/execute db/db-session (insert-layer-row layer-name)))


(defn drop-layer-table
  "Remove layer's corresponding row and table given by name."
  [layer-name]
  (hayt/drop-table (keyword layer-name)))


(defn delete-layer-row
  ""
  [layer-name]
  (hayt/delete :registry (hayt/where {:name layer-name})))


(defn remove!
  "Remove layer's corresponding row and table given by name."
  [layer-name]
  (alia/execute db/db-session (drop-layer-table layer-name))
  (alia/execute db/db-session (delete-layer-row layer-name)))


(defn lookup
  "Find layer by name."
  [layer-name]
  (hayt/select :registry (hayt/where {:name (name layer-name)})))


(defn lookup!
  "Find layer by name."
  [layer-name]
  (alia/execute db/db-session (lookup layer-name)))


(defn all
  ""
  []
  (hayt/select :registry))


(defn all!
  ""
  []
  (alia/execute db/db-session (all)))

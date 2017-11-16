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


(defn canonical-layer-name
  "Convert layer name to lower-case keyword"
  [layer-name]
  ;; separate on non-alphanumeric characters
  (try
    (csk/->SCREAMING_SNAKE_CASE layer-name :separator #"[\W]+")
    (catch RuntimeException cause
      (let [msg (format "could not derive canonical layer name for '%s'" layer-name)]
        (log/warn msg)
      (throw (ex-info msg {:layer-name layer-name} cause))))))


(defn create-layer-table
  "Create table to store layer's chips."
  [{name :name :as layer}]
  (hayt/create-table (keyword name)
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
    (let [layer (update layer :name canonical-layer-name)]
      (alia/execute db/db-session (create-layer-table layer))
      (alia/execute db/db-session (insert-layer-row layer))
      layer)
    (catch java.lang.RuntimeException cause
      (let [msg (format "could not create layer %s: %s" layer (.getMessage cause))]
        (throw (ex-info msg {} cause))))))


(defn drop-layer-table
  "Remove layer's corresponding row and table given by name."
  [layer-name]
  (hayt/drop-table (keyword layer-name)))


(defn delete-layer-row
  ""
  [layer-name]
  (hayt/delete :registry (hayt/where {:name (name layer-name)})))


(defn remove!
  "Remove layer's corresponding row and table given by name."
  [layer-name]
  (try
    (alia/execute db/db-session (drop-layer-table layer-name))
    (alia/execute db/db-session (delete-layer-row layer-name))
    true
    (catch java.lang.RuntimeException cause
      (let [msg (format "could not remove layer '%s'" layer-name)]
        (log/error msg)
        false))))


(defn lookup
  "Find layer by name."
  [layer-name]
  (hayt/select :registry (hayt/where {:name (canonical-layer-name layer-name)})))


(defn lookup!
  "Find layer by name."
  [layer-name]
  (->> (lookup layer-name)
       (alia/execute db/db-session)
       (first)))


(defn all
  ""
  []
  (hayt/select :registry))


(defn all!
  ""
  []
  (sort-by :name (alia/execute db/db-session (all))))


(defn search!
  "Search layers by tags."
  [params]
  (let [tagset (-> params :tags vector flatten set)
        layers (all!)]
    (if (params :tags)
      (filter #(clojure.set/subset? tagset (% :tags)) layers)
      layers)))

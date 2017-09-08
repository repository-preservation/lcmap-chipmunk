(ns lcmap.chipmunk.layer
  "Functions for managing layers and chips they contain."
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


(defn all
  ""
  []
  (hayt/select :layers))


(defn all!
  ""
  []
  (alia/execute db/db-session (all)))


(defn create
  "Create a row and table to store chips for a band of data."
  [layer-name]
  [(hayt/create-table (keyword layer-name)
                      (hayt/if-exists false)
                      (hayt/column-definitions default-columns)
                      (hayt/with default-options))
   (hayt/insert :layers (hayt/values {:name (name layer-name)}))])


(defn create!
  ""
  [layer-name]
  (count (map (partial alia/execute db/db-session) (create layer-name))))


(defn delete
  "Remove layer's corresponding row and table given by name."
  [layer-name]
  [(hayt/drop-table layer-name)
   (hayt/delete :layers (hayt/where {:name (name layer-name)}))])


(defn delete!
  "Remove layer's corresponding row and table given by name."
  [layer-name]
  (map (partial alia/execute db/db-session) (delete layer-name)))


(defn lookup
  "Find layer by name."
  [layer-name]
  (hayt/select :layers (hayt/where {:name (name layer-name)})))


(defn lookup!
  "Find layer by name."
  [layer-name]
  (alia/execute db/db-session (lookup layer-name)))


(defn insert-chip
  "Add chip to layer."
  [layer-name chip]
  (->> (select-keys chip [:x :y :acquired :data :source :hash])
       (hayt/values)
       (hayt/insert layer-name)))


(defn insert-chip!
  "Add chip to layer."
  [layer-name chip]
  (alia/execute db/db-session (insert-chip layer-name chip))
  chip)

(ns lcmap.chipmunk.setup
  "Initialize backing services."
  (:require [clojure.tools.logging :as log]
            [qbits.alia :as alia]
            [qbits.hayt :as hayt]
            [lcmap.chipmunk.config :as config]))

(set! *warn-on-reflection* true)

(defn create-keyspace
  "Returns a map that creates a keyspace suitable for development purposes;
   do not use it for an operational system."
  [keyspace-name]
  (hayt/create-keyspace keyspace-name
                        (hayt/if-exists false)
                        (hayt/with {:replication {"class" "SimpleStrategy" "replication_factor" "1"}
                                    :durable_writes true})))


(defn create-registry
  "Return a map that creates a table for saving layer metadata."
  []
  (hayt/create-table :registry
                     (hayt/if-exists false)
                     (hayt/column-definitions {:primary-key [:ubid]
                                               :ubid       :text
                                               :info       :text
                                               :tags       (hayt/frozen (hayt/set-type :text))
                                               :data_fill  :text
                                               :data_mask  (hayt/frozen (hayt/map-type :text :text))
                                               :data_range (hayt/frozen (hayt/list-type :int))
                                               :data_scale :float
                                               :data_shape (hayt/frozen (hayt/list-type :int))
                                               :data_type  :text
                                               :data_units :text})
                     (hayt/with {:compression {"sstable_compression" "LZ4Compressor"}
                                 :compaction  {"class" "LeveledCompactionStrategy"}})))


(defn create-grid
  "Return a map that creates a table that defines grids, used for operations like snapping points."
  []
  (hayt/create-table :grid
                     (hayt/if-exists false)
                     (hayt/column-definitions {:primary-key [:name]
                                               :name :text
                                               :proj :text
                                               :rx :double
                                               :ry :double
                                               :sx :double
                                               :sy :double
                                               :tx :double
                                               :ty :double})
                     (hayt/with {:compression {"sstable_compression" "LZ4Compressor"}
                                 :compaction  {"class" "LeveledCompactionStrategy"}})))


(defn create-inventory
  "Return a map that creates a table for tracking input source's metadata."
  []
  (hayt/create-table :inventory
                     (hayt/if-exists false)
                     (hayt/column-definitions {:primary-key [:source]
                                               :layer    :text
                                               :source   :text
                                               :url      :text
                                               :tile     :text
                                               :chips    :text
                                               :extra    :text})))


(defn create-inventory-tile-index
  ""
  []
  (hayt/create-index :inventory
                     :tile
                     (hayt/index-name :inventory_tile_ix)
                     (hayt/if-exists false)))


(defn create-inventory-layer-index
  ""
  []
  (hayt/create-index :inventory
                     :layer
                     (hayt/index-name :inventory_layer_ix)
                     (hayt/if-exists false)))

(defn create-inventory-materialized-view
  ""
  []
  (str "CREATE MATERIALIZED VIEW IF NOT EXISTS inventory_by_tile 
        AS SELECT tile, source FROM inventory 
        WHERE tile IS NOT null AND source IS NOT null 
        PRIMARY KEY(tile, source);"))

(defn init
  "Create table to store metadata about each layer."
  []
  (let [cluster (alia/cluster (config/alia-config))
        session (alia/connect cluster)
        ks-name (config/config :db-keyspace)]
    (log/debug "chipmunk db setup started")
    (try
      (log/debugf "creating keyspace '%s' if needed" ks-name)
      (alia/execute session (create-keyspace ks-name))
      (catch java.lang.RuntimeException cause
        (log/errorf "could not create chipmunk keyspace '%s'" ks-name)
        :fail))
    (try
      (log/debugf "using keyspace '%s'" ks-name)
      (alia/execute session (hayt/use-keyspace ks-name))
      (log/debugf "creating registry")
      (alia/execute session (create-registry))
      (log/debugf "creating inventory")
      (alia/execute session (create-inventory))
      (log/debugf "creating grid")
      (alia/execute session (create-grid))
      (log/debugf "creating inventory's tile index")
      (alia/execute session (create-inventory-tile-index))
      (log/debugf "creating inventory's layer index")
      (alia/execute session (create-inventory-layer-index))
      (log/debugf "creating inventory materialized view")
      (alia/execute session (create-inventory-materialized-view))
      :done
      (catch java.lang.RuntimeException cause
        (log/errorf "could not create chipmunk's default tables.")
        (throw (ex-info (.getMessage cause) {} cause)))
      (finally
        (alia/shutdown session)
        (alia/shutdown cluster)
        (log/debugf "chipmunk db setup finished")))))


(defn nuke
  ""
  [verified-ks-name]
  (let [cluster (alia/cluster (config/alia-config))
        session (alia/connect cluster)
        ks-name (config/config :db-keyspace)]
    (if (= ks-name verified-ks-name)
      (try
        (log/debugf "remove chipmunk keyspace '%s'" ks-name)
        (alia/execute session (hayt/drop-keyspace ks-name))
        (catch java.lang.RuntimeException cause
          (log/errorf "could not remove keyspace '%s': %s" ks-name (.getMessage cause)))
        (finally
          (alia/shutdown session)
          (alia/shutdown cluster)
          (log/debugf "chipmunk db teardown finished")))
      (throw (ex-info "You cannot nuke the DB unless the keyspace you provide matches the configured keyspace." {})))))

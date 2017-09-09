(ns lcmap.chipmunk.setup
  "Initialize backing services."
  (:require [clojure.tools.logging :as log]
            [qbits.alia :as alia]
            [qbits.hayt :as hayt]
            [lcmap.chipmunk.config :as config]))


(defn create-keyspace
  ""
  [keyspace-name]
  (hayt/create-keyspace keyspace-name
                        (hayt/if-exists false)
                        (hayt/with {:replication {"class" "SimpleStrategy" "replication_factor" "1"}
                                    :durable_writes true})))


(defn create-registry
  ""
  []
  (hayt/create-table :registry
                     (hayt/if-exists false)
                     (hayt/column-definitions {:primary-key [:name] :name :text})
                     (hayt/with {:compression {"sstable_compression" "LZ4Compressor"}
                                 :compaction  {"class" "LeveledCompactionStrategy"}})))


(defn create-inventory
  ""
  []
  (hayt/create-table :inventory
                     (hayt/if-exists false)
                     (hayt/column-definitions {:primary-key [[:layer,:source]]
                                               :layer    :text
                                               :source   :text
                                               :url      :text
                                               :tile     :text
                                               :chips    :text})))


(defn create-inventory-tile-index
  ""
  []
  (hayt/create-index :inventory
                     :tile
                     (hayt/index-name :inventory_tile_ix)
                     (hayt/if-exists false)))


(defn init
  "Create table to store metadata about each layer."
  []
  (let [cluster (alia/cluster (config/alia-config))
        session (alia/connect cluster)
        ks-name (config/config :db-keyspace)]
    (log/debug "chipmunk db setup started")
    (alia/execute session (create-keyspace ks-name))
    (alia/execute session (hayt/use-keyspace ks-name))
    (alia/execute session (create-registry))
    (alia/execute session (create-inventory))
    (alia/execute session (create-inventory-tile-index))
    (alia/shutdown session)
    (alia/shutdown cluster)
    (log/debug "chipmunk db setup finished")
    :done))

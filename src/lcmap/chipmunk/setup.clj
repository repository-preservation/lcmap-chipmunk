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
  [registry-name]
  (hayt/create-table registry-name
                     (hayt/if-exists false)
                     (hayt/column-definitions {:primary-key [:name] :name :text})
                     (hayt/with {:compression {"sstable_compression" "LZ4Compressor"}
                                 :compaction  {"class" "LeveledCompactionStrategy"}})))


(defn init
  "Create table to store metadata about each layer."
  []
  (let [cluster (alia/cluster (config/alia-config))
        session (alia/connect cluster)
        ks-name (config/config :db-keyspace)
        tb-name :layers]
    (log/debug "chipmunk db setup started")
    (alia/execute session (create-keyspace ks-name))
    (alia/execute session (hayt/use-keyspace ks-name))
    (alia/execute session (create-registry tb-name))
    (alia/shutdown session)
    (alia/shutdown cluster)
    (log/debug "chipmunk db setup finished")
    :done))

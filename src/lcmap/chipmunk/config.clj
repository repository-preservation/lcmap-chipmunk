(ns lcmap.chipmunk.config
  "Configuration related functions.

  Values are obtained from ENV variables and/or profiles.clj. These are:

  | ENV                      | Description                 |
  | ------------------------ | --------------------------- |
  | `HTTP_PORT`              | Chipmunk's HTTP listener    |
  | `HTTP_TIMEOUT_MILLIS`    | HTTP client timeout         |
  | `DB_HOST`                | Cassandra node (just one)   |
  | `DB_USER`                | Cassandra username          |
  | `DB_PASS`                | Cassandra password          |
  | `DB_PORT`                | Cassandra cluster port      |
  | `DB_KEYSPACE`            | Chipmunk's keyspace name    |
  | `DB_READ_TIMEOUT_MILLIS` | Cassandra read timeout      |
  "
  (:require [environ.core :as environ]
            [lcmap.chipmunk.util :as util]
            [mount.core :as mount]
            [qbits.alia.policy.load-balancing :as lb]))

(set! *warn-on-reflection* true)

(def config (select-keys environ/env [:db-host
                                      :db-user
                                      :db-pass
                                      :db-port
                                      :db-keyspace
                                      :http-port
                                      :http-timeout-millis
                                      :db-read-timeout-millis]))


(defn string->vector
  "Split string on comma into vector"
  [s]
  (->> (clojure.string/split s #"[, ]+")
       (map clojure.string/trim)
       (vec)))


(defn http-options
  []
  {:timeout (util/numberize(:http-timeout-millis config))})


(defn alia-config
  "Produce alia specific configuration from environment.
   Uses round-robin load balancing policy.  If running across
   multiple datacenters, use dc-aware-round-robin policy instead."
  []
  {:contact-points (-> :db-host config string->vector)
   :credentials    {:user (:db-user config)
                    :password (:db-pass config)}
   :query-options  {:consistency :quorum}
   :load-balancing-policy (lb/round-robin-policy)
   :socket-options {:read-timeout-millis
                    (util/numberize (or (-> config :db-read-timeout-millis) nil))}})

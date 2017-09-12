(ns lcmap.chipmunk.config
  "Configuration related functions.

  Values are obtained from ENV variables and/or profiles.clj. These are:

  | ENV            | Description                 |
  | -------------- | --------------------------- |
  | `HTTP_PORT`    | Chipmunk's HTTP listener    |
  | `DB_HOST`      | Cassandra node (just one)   |
  | `DB_USER`      | Cassandra username          |
  | `DB_PASS`      | Cassandra password          |
  | `DB_PORT`      | Cassandra cluster port      |
  | `DB_KEYSPACE`  | Chipmunk's keyspace name    |
  "
  (:require [environ.core :as environ]
            [mount.core :as mount]))


(def config (select-keys environ/env [:db-host :db-user :db-pass :db-port :db-keyspace :http-port]))


(defn string->vector
  "Split string on comma into vector"
  [s]
  (->> (clojure.string/split s #"[, ]+")
       (map clojure.string/trim)
       (vec)))


(defn alia-config
  "Produce alia specific configuration from environment."
  []
  {:contact-points (-> :db-host config string->vector)
   :credentials    {:user (:db-user config)
                    :password (:db-pass config)}
   :query-options  {:consistency :quorum}})

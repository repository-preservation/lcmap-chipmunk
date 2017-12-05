(ns lcmap.chipmunk.db
  "Cassandra connections and helper functions."
  (:require [clojure.tools.logging :as log]
            [mount.core :refer [defstate] :as mount]
            [qbits.alia :as alia]
            [qbits.alia.codec.default :as default-codec]
            [lcmap.chipmunk.config :as config])
  (:import org.joda.time.DateTime))


;; ## Overview
;;
;; This namespace defines the state and behavior of connections to
;; the Cassandra cluster. It relies on values `lcmap.chipmunk.config`
;; for specific values such as hostnames and credentials.
;;

;; ## Joda Support
;;
;; Enabling encoding of Joda types provides a degree of convenience.
;;

(extend-protocol default-codec/Encoder
  org.joda.time.DateTime (encode [x] (.toDate x)))


;; ## Declarations
;;
;;

(declare db-cluster db-session)


;; ## db-cluster
;;
;; After start `db-cluster` refers to com.datastax.driver.core.Cluster,
;; an object that maintains general information about the cluster; use
;; db-session to execute queries.
;;

(defn db-cluster-start
  "Open cluster connection."
  []
  (let [db-cfg (config/alia-config)]
    (log/debugf "start db cluster connection")
    (alia/cluster db-cfg)))


(defn db-cluster-stop
  "Shutdown cluster connection."
  []
  (log/debugf "stop db cluster connection")
  (alia/shutdown db-cluster))


(defstate db-cluster
  :start (db-cluster-start)
  :stop  (db-cluster-stop))


;; ## db-session
;;
;; After start `db-session` refers to a com.datastax.driver.core.SessionManager
;; object that can be used to execute queries.
;;
;; _WARNING: Do not use the same session for multiple keyspaces, functions
;; that rely on this state expect a stable keyspace name!_
;;

(defn db-session-start
  "Create session that uses the default keyspace."
  []
  (log/debugf "start db session")
  (alia/connect db-cluster (:db-keyspace config/config)))


(defn db-session-stop
  "Close Cassandra session."
  []
  (log/debugf "stop db session")
  (alia/shutdown db-session))


(defstate db-session
  :start (db-session-start)
  :stop  (db-session-stop))

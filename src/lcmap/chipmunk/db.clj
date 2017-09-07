(ns lcmap.chipmunk.db
  "Cassandra connections and helper functions."
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [mount.core :refer [defstate] :as mount]
            [qbits.alia :as alia]
            [lcmap.chipmunk.config :as config]))


(declare db-cluster db-session)


(defn db-cluster-start
  "Open cluster connection.

  See also `db-session`."
  []
  (let [db-cfg (config/alia-config)]
    (log/debugf "start db cluster connection")
    (alia/cluster db-cfg)))


(defn db-cluster-stop
  "Shutdown cluster connection."
  []
  (log/debugf "stop db cluster connection")
  (alia/shutdown db-cluster))

;; After start this refers to com.datastax.driver.core.Cluster, an
;; object that maintains general information about the cluster; use
;; db-session to execute queries.

(defstate db-cluster
  :start (db-cluster-start)
  :stop  (db-cluster-stop))


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


;; After start this will refer to a com.datastax.driver.core.SessionManager
;; object that can be used to execute queries.
;;
;; WARNING: Do not use the same session for multiple keyspaces, functions
;; that rely on this state expect a stable keyspace name!
;;
(defstate db-session
  :start (db-session-start)
  :stop  (db-session-stop))

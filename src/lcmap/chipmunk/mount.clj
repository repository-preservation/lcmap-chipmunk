(ns lcmap.chipmunk.mount
  (:require [clojure.tools.logging :as log]
            [mount.core :as mount]
            [lcmap.chipmunk.config :as config]))


(declare http-server)


(defn http-start []
  (let [port (-> config/config :http-port Integer/parseInt)]
    (log/debugf "start http server on port %s" port)
    (server/run-server #'app {:port port})))


(defn http-stop []
  (log/debug "stop http server")
  (http-server))


(mount/defstate http-server
  :start (http-start)
  :stop  (http-stop))

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


(defstate db-session
  :start (db-session-start)
  :stop  (db-session-stop))

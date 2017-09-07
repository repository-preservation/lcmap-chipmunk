(ns lcmap.chipmunk.config
  ""
  (:require [environ.core :as environ]
            [mount.core :as mount]))


(def config (select-keys environ/env [:db-host :db-user :db-pass :db-port :db-keyspace :http-port]))


(defn alia-config
  "Produce alia specific configuration from environment."
  []
  {:contact-points (-> :db-host config vector)
   :credentials    {:user (:db-user config)
                    :password (:db-pass config)}
   :query-options  {:consistency :quorum}})

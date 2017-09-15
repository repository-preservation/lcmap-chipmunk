(ns lcmap.chipmunk.fixtures
  (:require [clojure.test :refer :all]
            [clojure.edn :as edn]
            [clojure.tools.logging :as log]
            [mount.core :as mount]
            [lcmap.chipmunk.http]
            [lcmap.chipmunk.db]
            [lcmap.chipmunk.setup :as setup]
            [lcmap.chipmunk.registry :as registry]))


(defn schema-fixture [f]
  (log/debugf "schema-fixture up")
  (setup/init)
  (f)
  (setup/nuke "chipmunk_test")
  (log/debugf "schema-fixture down"))


(defn mount-fixture [f]
  (log/debugf "mount-fixture up")
  (mount/start)
  (f)
  (mount/stop)
  (log/debugf "mount-fixture down"))


(defn registry-fixture [f]
  (let [layers (-> "test/resources/registry.fixture.edn" slurp edn/read-string)]
    (log/debugf "registry-fixture up")
    (into [] (map registry/add!) layers)
    (f)
    (into [] (comp (map :name) (map registry/remove!)) layers)
    (log/debugf "registry-fixture down")))


(def all-fixtures (join-fixtures [schema-fixture mount-fixture registry-fixture]))

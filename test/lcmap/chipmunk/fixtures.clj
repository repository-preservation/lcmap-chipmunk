(ns lcmap.chipmunk.fixtures
  (:require [clojure.test :refer :all]
            [clojure.edn :as edn]
            [clojure.tools.logging :as log]
            [mount.core :as mount]
            [lcmap.chipmunk.http]
            [lcmap.chipmunk.db]
            [lcmap.chipmunk.setup :as setup]
            [lcmap.chipmunk.registry :as registry]
            [lcmap.chipmunk.grid :as grid]))


(defn schema-fixture [f]
  (log/tracef "schema-fixture up")
  (setup/init)
  (f)
  (setup/nuke "chipmunk_test")
  (log/tracef "schema-fixture down"))


(defn mount-fixture [f]
  (log/tracef "mount-fixture up")
  (mount/start)
  (f)
  (mount/stop)
  (log/tracef "mount-fixture down"))


(defn registry-fixture [f]
  (let [layers (-> "test/resources/registry.fixture.edn" slurp edn/read-string)]
    (log/tracef "registry-fixture up")
    (into [] (map registry/add!) layers)
    (f)
    (into [] (comp (map :ubid) (map registry/remove!)) layers)
    (log/tracef "registry-fixture down")))


(defn grid-fixture [f]
  (let [grids (-> "test/resources/grid.fixture.edn" slurp edn/read-string)]
    (log/tracef "grid-fixture up")
    (into [] (map grid/add!) grids)
    (f)
    (into [] (comp (map :name) (map grid/remove!)) grids)
    (log/tracef "grid-fixture down")))


(def all-fixtures (join-fixtures [schema-fixture mount-fixture registry-fixture grid-fixture]))

(ns lcmap.chipmunk.inventory
  "Functions for managing source data. These are created as
   part of the ingest process."
  (:require [clojure.tools.logging :as log]
            [cheshire.core :as json]
            [qbits.alia :as alia]
            [qbits.hayt :as hayt]
            [lcmap.chipmunk.db :as db]))


(defn insert-source
  "Add source info to inventory."
  [source]
  (let [relevant (select-keys source [:source :layer :tile :chips :url])]
    (->> (update relevant :chips json/encode)
         (hayt/values)
         (hayt/insert :inventory))))


(defn save!
  "Add info about a source to the inventory."
  [source]
  (alia/execute db/db-session (insert-source source))
  source)


(defn ->source
  ""
  [result]
  (update result :chips json/decode))


(defn lookup
  ""
  [layer-id source-id]
  (hayt/select :inventory (hayt/where {:layer layer-id
                                       :source source-id})))


(defn lookup!
  "Retrieve info about a source from the inventory."
  [layer-id source-id]
  (->> (lookup layer-id source-id)
       (alia/execute db/db-session)
       (map ->source)))

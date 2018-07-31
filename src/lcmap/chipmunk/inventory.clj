(ns lcmap.chipmunk.inventory
  "Functions for managing source data. These are created as
   part of the ingest process."
  (:require [clojure.tools.logging :as log]
            [clojure.spec.alpha :as spec]
            [cheshire.core :as json]
            [qbits.alia :as alia]
            [qbits.hayt :as hayt]
            [lcmap.chipmunk.db :as db]
            [lcmap.chipmunk.util :as util]))

(set! *warn-on-reflection* true)

(spec/def ::tile string?)
(spec/def ::layer string?)
(spec/def ::source string?)
(spec/def ::url string?)
(spec/def ::search-query (spec/keys :req-un [(or ::source ::url)]))
(spec/def ::source-query (spec/keys :req-un [(or ::tile ::url)]))


(defn insert-source
  "Add source info to inventory."
  [source]
  (let [relevant (select-keys source [:source :layer :url :tile :chips :extra])]
    (as-> relevant source
      (update source :chips json/encode)
      (update source :extra json/encode)
      (hayt/values source)
      (hayt/insert :inventory source))))


(defn save
  "Add info about a source to the inventory."
  [source]
  (alia/execute db/db-session (insert-source source))
  source)


(defn ->source
  "Deserialize JSON used to describe what chips were ingested."
  [result]
  (cond-> result
    (result :chips) (update :chips json/decode)
    (result :extra) (update :extra json/decode)))


(defn identify
  "Derive a source ID from a URL."
  [url]
  (-> url
      (java.net.URI.)
      (.getPath)
      (java.io.File.)
      (.getName)))


(defn url-to-source
  [params]
  (if-let [url (params :url)]
    (-> params
        (assoc :source (identify url))
        (dissoc :url))
    params))


(defn search
  "Query inventory by source."
  [{:keys [:source :url :only] :as params}]
  (let [params (util/check! ::search-query (url-to-source params))
        columns (or (some-> only list flatten) "*")]
    (->> (hayt/select :inventory
                      (hayt/where (dissoc params :only))
                      (apply hayt/columns columns))
         (alia/execute db/db-session)
         (map ->source))))


(defn tile->sources
  "Query inventory materialized view by tile"
  [{:keys [:tile] :as params}]
  (let [params (util/check! ::source-query params)
        columns "*"]
    (->> (hayt/select :inventory_by_tile
                      (hayt/where params)
                      (apply hayt/columns columns))
         (alia/execute db/db-session)
         (map ->source))))

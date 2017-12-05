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


(spec/def ::tile string?)
(spec/def ::layer string?)
(spec/def ::source string?)
(spec/def ::url string?)
(spec/def ::query (spec/keys :req-un [(or ::tile ::layer ::source ::url)]))


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
  (-> result
      (update :chips json/decode)
      (update :extra json/decode)))


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
  "Query inventory by tile, layer, and/or source."
  [{:keys [:tile :layer :source :url] :as params}]
  (let [params (util/check! ::query (url-to-source params))]
    (->> (hayt/select :inventory
                      (hayt/where params)
                      (hayt/columns :layer :source :tile :url :chips :extra))
         (alia/execute db/db-session)
         (map ->source))))

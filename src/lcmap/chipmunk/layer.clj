(ns lcmap.chipmunk.layer
  "Functions for managing the layer's data. To add or remove
   layers, use functions in `lcmap.chipmunk.registry`.

   These functions do no transformation on chip data, aside from
   extracting information that can be stored in the DB."
  (:require [clojure.tools.logging :as log]
            [qbits.alia :as alia]
            [qbits.hayt :as hayt]
            [lcmap.chipmunk.db :as db]))


(defn insert-chip
  "Add chip to layer."
  [{:keys [:layer] :as chip}]
  (->> (select-keys chip [:source :x :y :acquired :data :hash])
       (hayt/values)
       (hayt/insert (keyword layer))))


(defn insert-chip!
  "Add chip to layer."
  [chip]
  (alia/execute db/db-session (insert-chip chip)))


(defn save!
  "Add all chips to layer."
  [chips]
  (dorun (map insert-chip! chips)))

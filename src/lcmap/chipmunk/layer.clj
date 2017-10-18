(ns lcmap.chipmunk.layer
  "Functions for managing the layer's data. To add or remove
   layers, use functions in `lcmap.chipmunk.registry`.

   These functions do no transformation on chip data, aside from
   extracting information that can be stored in the DB."
  (:require [clojure.tools.logging :as log]
            [qbits.alia :as alia]
            [qbits.hayt :as hayt]
            [lcmap.chipmunk.db :as db]
            [lcmap.chipmunk.util :as util]))


(defn insert-chip
  "Add chip to layer."
  [{:keys [:layer] :as chip}]
  (->> (select-keys chip [:source :x :y :acquired :data :hash])
       (hayt/values)
       (hayt/insert (keyword layer))))


(defn insert-chip!
  "Add chip to layer."
  [chip]
  (alia/execute db/db-session (insert-chip chip))
  (assoc chip :saved true))


(defn save!
  "Add all chips to layer."
  [chips]
  (into [] (map insert-chip! chips)))


(defn lookup
  "Get chips matching query."
  [layer-name query]
  (hayt/select (keyword layer-name)
               (-> (select-keys query [:x :y])
                   (update :x util/numberize)
                   (update :y util/numberize)
                   (hayt/where))))


(defn lookup!
  "Get chips matching query."
  [layer-name query]
  (alia/execute db/db-session (lookup layer-name query)))

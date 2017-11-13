(ns lcmap.chipmunk.chips
  "Functions for managing chip data."
  (:require [clojure.tools.logging :as log]
            [clojure.spec.alpha :as spec]
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
  (let [i (-> query :acquired util/intervalize bean)
        x (-> query :x util/numberize)
        y (-> query :y util/numberize)]
    (hayt/select (keyword layer-name)
                 (hayt/where [[= :x x]
                              [= :y y]
                              [>= :acquired (-> :start i str)]
                              [<= :acquired (-> :end i str)]]))))


(defn lookup!
  "Get chips matching query."
  [params]
  ;; In order to preserve compatability with previous consumers
  ;; of similar REST APIs the ubid of each chip needs to be set.
  (let [ubid  (:ubid params)
        query (dissoc params :ubid)]
    (alia/execute db/db-session (lookup ubid query))))

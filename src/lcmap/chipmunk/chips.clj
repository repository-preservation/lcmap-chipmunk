(ns lcmap.chipmunk.chips
  "Functions for managing chip data."
  (:require [clojure.tools.logging :as log]
            [clojure.spec.alpha :as spec]
            [clojure.spec.test.alpha :as stest]
            [qbits.alia :as alia]
            [qbits.hayt :as hayt]
            [qbits.hayt.codec.joda-time :as joda-time]
            [digest :as digest]
            [lcmap.chipmunk.db :as db]
            [lcmap.chipmunk.util :as util]
            [lcmap.chipmunk.registry :as registry]
            [lcmap.chipmunk.grid :as grid]))


;; ## Overview
;;
;; A chip is raw raster data, associated with a layer, x and y
;; coordinate in a uniform reference system, acquisition date
;; and source.
;;
;; All chips for a particular layer (e.g. LC08_SRB1) are stored
;; in a single table.
;;
;; Information about the data type and shape are stored in the
;; registry, information about the original source is stored in
;; the inventory. In order to calculate the x and y coordinate
;; for a chip, use the `snap` function defined in the grid ns.
;;


;; ## Inserting Chips
;;
;; Saving a chip is a simple operation. In order to avoid problems
;; with chips that have additional properties that aren't persisted,
;; the function used to build the insert statement explicitly
;; selects that values to be persisted.
;;

(defn insert-chip
  "Build query to add chip to layer."
  [{:keys [:layer] :as chip}]
  (->> (select-keys chip [:source :x :y :acquired :data :hash])
       (hayt/values)
       (hayt/insert (keyword layer))))


(defn insert-chip!
  "Add chip to layer."
  [chip]
  (try
    (alia/execute db/db-session (insert-chip chip))
    (assoc chip :saved true)
    (catch java.lang.RuntimeException cause
      (let [msg (format "could not insert chip")]
        (throw (ex-info msg (dissoc chip :data) cause))))))


;; This is a convenience function that exists to keep ingest
;; code concise. It operates on a sequence of chips.
;;

(defn save
  "Add all chips to layer."
  [chips]
  (into [] (map insert-chip! chips)))


;; This spec is used to validate chip queries. It uses conformers
;; to convert values from one type to another; e.g. string values
;; for points into numbers.
;;

(spec/def ::x (spec/conformer util/numberizer))
(spec/def ::y (spec/conformer util/numberizer))
(spec/def ::acquired (spec/conformer util/intervalize))
(spec/def ::ubid string?)
(spec/def ::query (spec/keys :req-un [::x ::y ::acquired ::ubid]))

(defn etag
  "Produce an etag for a set of chips"
  [chips]
  (let [hashes (map (juxt :x :y :acquired :hash) chips)]
    (digest/md5 (clojure.string/join "+" hashes))))


(defn search
  "Get chips matching query."
  [{:keys [:ubid :x :y :acquired] :as query}]
  (hayt/select (keyword ubid)
               (hayt/where [[= :x (long x)]
                            [= :y (long y)]
                            [>= :acquired (-> acquired bean :start str)]
                            [<= :acquired (-> acquired bean :end str)]])))


(defn search!
  "Get chips matching query; handles snapping arbitrary x/y to chip x/y."
  [params]
  (let [params  (util/check! ::query params)
        grid    (grid/search "chip")
        [sx sy] (grid/proj-snap params grid)]
    (->> (assoc params :x sx :y sy)
         (search)
         (alia/execute db/db-session))))

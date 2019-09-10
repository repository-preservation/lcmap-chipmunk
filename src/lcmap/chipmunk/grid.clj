(ns lcmap.chipmunk.grid
  "Provides functions for working with grids."
  (:require [clojure.core.matrix :as matrix]
            [qbits.alia :as alia]
            [qbits.hayt :as hayt]
            [lcmap.chipmunk.db :as db]
            [lcmap.chipmunk.util :as util]))

(set! *warn-on-reflection* true)

;; ## DB Functions
;;


(defn add!
  "Add a grid to the DB."
  [grid]
  (->> (hayt/values grid)
       (hayt/insert :grid)
       (alia/execute db/db-session))
  grid)


(defn all!
  "Get all defined grids."
  []
  (->> (hayt/select :grid)
       (alia/execute db/db-session)))


(defn search
  "Find a grid by name."
  [name]
  (->> (hayt/where [[= :name name]])
       (hayt/select :grid)
       (alia/execute db/db-session)
       (first)))


(defn remove!
  "Remove a grid from the DB."
  [name]
  (->> (hayt/where [[= :name name]])
       (hayt/delete :grid)
       (alia/execute db/db-session)))



;; ## Utility Functions
;;


(matrix/set-current-implementation :vectorz)



(defn transform-matrix
  "Produce transform matrix from given layer's grid-spec."
  [layer]
  (let [rx (layer :rx)
        ry (layer :ry)
        sx (layer :sx)
        sy (layer :sy)
        tx (layer :tx)
        ty (layer :ty)]
    (comment (println "rx:" rx)
    (println "ry:" ry)
    (println "sx:" sx)
    (println "sy:" sy)
    (println "tx:" tx)
    (println "ty:" ty))

    [[(/ rx sx) 0          (/ tx sx)]
     [0         (/ ry sy)  (/ ty sy)]
     [0         0          1.0 ]]))


(defn point-matrix
  "Produce a homogeneous matrix from a map containing an :x and :y point."
  [params]
  (let [x (-> params :x util/numberize)
        y (-> params :y util/numberize)]
    [[x]
     [y]
     [1]]))

(defn proj-snap-fn
  "Create fn for finding 'on-the-grid' projection coordinates."
  [grid]
  (fn [point]
    ;; rst = reflection, scale, translation
    ;; rsti = rst-inverse
    ;; sx, sy = snapped-x, snapped-y
    (let [rst        (transform-matrix grid)
          rsti       (matrix/inverse rst)
          orig-pt    (point-matrix point)
          multiplied (matrix/mmul rst orig-pt)
          floated    (map (fn [x] (vector (float (first x)))) multiplied)
          grid-pt    (matrix/floor floated)
          snap-pt    (matrix/round (matrix/mmul rsti grid-pt))
          [[sx] [sy] [_]] snap-pt]
      [sx sy])))


(defn proj-snap
  "Determine projection system point that is 'on-the-grid'"
  [point grid]
  ((proj-snap-fn grid) point))


(defn grid-snap-fn
  "Create a fn that calculates grid units of a point (not projection units)."
  [grid]
  (fn [point]
    ;; rst = reflection, scale, translation
    ;; rsti = rst-inverse
    ;; sx, sy = snapped-x, snapped-y
    (let [rst             (transform-matrix grid)
          orig-pt         (point-matrix point)
          multiplied      (matrix/mmul rst orig-pt)
          floated         (map (fn [x] (vector (float (first x)))) multiplied)
          grid-pt         (matrix/floor floated)
          [[sx] [sy] [_]] grid-pt]
      [sx sy])))


(defn grid-snap
  "Find the point in grid units (not projection units)."
  [point grid]
  ((grid-snap-fn grid) point))


(defn snap
  ""
  [point grid]
  [(grid :name)
   {:proj-pt (proj-snap point grid)
    :grid-pt (grid-snap point grid)}])


(defn near
  ""
  [params grid]
  (let [[x y] (proj-snap params grid)
        dx (grid :sx)
        dy (grid :sy)]
    {(grid :name)
     (for [x (range (- x dx) (+ x dx 1) dx)
           y (range (- y dy) (+ y dy 1) dy)]
       (last (snap {:x x :y y} grid)))}))

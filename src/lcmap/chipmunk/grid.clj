(ns lcmap.chipmunk.grid
  "Provides functions for working with gridded data."
  (:require [clojure.core.matrix :as matrix]
            [lcmap.chipmunk.util :as util]))


(matrix/set-current-implementation :vectorz)


(defn transform-matrix
  "Produce transform matrix from given layer's grid-spec."
  [layer]
  (let [rx (layer :grid_rx)
        ry (layer :grid_ry)
        sx (layer :grid_sx)
        sy (layer :grid_sy)
        tx (layer :grid_tx)
        ty (layer :grid_ty)]
    [[(/ rx sx)        0  (/ tx sx)]
     [       0  (/ ry sy) (/ ty sy)]
     [       0         0       1.0 ]]))


(defn point-matrix
  "Produce a homogeneous matrix from a map containing an :x and :y point."
  [params]
  (let [x (-> params :x util/numberize)
        y (-> params :y util/numberize)]
    [[x]
     [y]
     [1]]))


(defn snap-fn
  "Create a snapping fn for given layer."
  [layer]
  (fn [point]
    ;; rst = rotate, scale, translate
    ;; rsti = rst-inverse
    ;; sx, sy = snapped-x, snapped-y
    (let [rst        (transform-matrix layer)
          rsti       (matrix/inverse rst)
          orig-pt    (point-matrix point)
          grid-pt    (matrix/floor (matrix/mmul rst orig-pt))
          snap-pt    (matrix/round (matrix/mmul rsti grid-pt))
          [[sx] [sy] [_]] snap-pt]
      [sx sy])))


(defn snap
  "Find snapped x and y for given point and layer."
  [point layer]
  ((snap-fn layer) point))

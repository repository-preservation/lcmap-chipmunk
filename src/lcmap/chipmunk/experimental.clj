(ns lcmap.chipmunk.experimental
  "Experimental functions."
  (:require [clojure.core.matrix :as matrix]
            [lcmap.chipmunk.util :as util]))


(matrix/set-current-implementation :vectorz)


(defn transform-matrix
  "Produce a transform matrix to convert projection coordinate
   into grid coordinate."
  [layer]
  (let [rx (layer :grid_rx)
        ry (layer :grid_ry)
        sx (layer :grid_sx)
        sy (layer :grid_sy)
        tx (layer :grid_tx)
        ty (layer :grid_ty)]
    [[(* rx sx)        0  (* tx sx)]
     [       0  (* ry sy) (* ty sy)]
     [       0         0         1]]))


(defn point-matrix
  "Produce a homogeneous matrix from a map containing an :x and :y point property."
  [params]
  (let [x (-> params :x util/numberize)
        y (-> params :y util/numberize)]
    [[x]
     [y]
     [1]]))


(defn snap-matrix
  "Find chip x and y for given x and y."
  [params layer]
  (let [rst        (transform-matrix layer)
        rsti       (matrix/inverse rst)
        orig-pt    (point-matrix params)
        grid-pt    (matrix/mmul rst orig-pt)
        snap-pt    (matrix/mmul rsti grid-pt)
        [[cx] [cy] [_]] snap-pt]
    {:grid-point grid-pt :snap-point snap-pt}))

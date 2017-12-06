(ns lcmap.chipmunk.grid-test
  (:require [clojure.test :refer :all]
            [lcmap.chipmunk.grid :refer :all]))


(def chip-grid {:rx 1.0 :ry -1.0
                :sx 3000.0 :sy 3000.0
                :tx 2565585.0 :ty 3314805.0})


(deftest snap-matrix-test
  (testing "point is 'in'"
    ;; This point is 1 meter south-east of a grid point.
    (let [[sx sy] (proj-snap {:x -2115584.0 :y 1964804.0} chip-grid)]
      (is (= -2115585.0 sx))
      (is (=  1964805.0 sy))))
  (testing "point is 'on' the grid"
    ;; This point is exactly on the grid.
    (let [[sx sy] (proj-snap {:x -2115585.0 :y 1964805.0} chip-grid)]
      (is (= -2115585.0 sx))
      (is (=  1964805.0 sy))))
  (testing "point is 'out'"
    ;; This point is 1 meter north-west of a grid point
    ;; Notice that the compared value is different than
    ;; the previous two tests.
    (let [[sx sy] (proj-snap {:x -2115586.0 :y 1964806.0} chip-grid)]
      (is (= -2118585.0 sx))
      (is (=  1967805.0 sy)))))

(ns lcmap.chipmunk.grid-test
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.test :refer :all]
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


;; snap function failures were discovered in the Alaska grid.  When coordinates
;; were supplied that fell exactly on the west boundary of *some* tiles & chips,
;; snap was returning the value for the chip or tile directly west of the
;; correct tile or chip.  This was due to floating point precision
;; of values being passed to the floor function within proj-snap-fn and grid-snap-fn.
;;
;; Wrong tile grid corners are produced for tiles h2v0 thru h3v13 (inclusive)
;; Wrong chip corners are output for chips h28,v0 thru h256v699 (inclusive)
;;
;; No such failures presented within the CONUS or HI grids.


(def alaska-tile-grid {:rx 1.0 :ry -1.0
                       :sx 150000.0 :sy 150000.0
                       :tx 851715.0 :ty 2474325.0})

(def alaska-chip-grid {:rx 1.0 :ry -1.0
                       :sx 3000.0 :sy 3000.0
                       :tx 851715.0 :ty 2474325.0})

(defn snap-csv-entry
  [the-grid csv-line]
  (let [x  (Integer/parseInt (nth csv-line 0))
        y  (Integer/parseInt (nth csv-line 1))
        h  (Integer/parseInt (nth csv-line 2))
        v  (Integer/parseInt (nth csv-line 3))
        hv (grid-snap {:x x :y y} the-grid)]

    {:h-in h :v-in v :h-out (int (first hv)) :v-out (int (second hv))}))

(defn snapped
  [the-grid the-file]
  (->> the-file
       (slurp)
       (csv/read-csv)
       (rest)
       (map (partial snap-csv-entry the-grid))))

(deftest alaska-tile-grid-snap-test
  (testing "snapped to tile grid is correct"
    (doseq [t (snapped alaska-tile-grid "test/resources/alaska_tiles.csv")]
      (let [hin  (:h-in t)
            hout (:h-out t)
            vin  (:v-in t)
            vout (:v-out t)]
        (when (or (not (= hin hout)) (not (= vin vout)))
          (println t))

        (is (and (= hin hout) (= vin vout)))))))

(deftest alaska-chip-grid-snap-test
  (testing "snapped to chip grid is correct"
    (doseq [t (snapped alaska-chip-grid "test/resources/alaska_chips.csv")]
      (let [hin  (:h-in t)
            hout (:h-out t)
            vin  (:v-in t)
            vout (:v-out t)]
        (when (or (not (= hin hout)) (not (= vin vout)))
          (println t))

        (is (and (= hin hout) (= vin vout)))))))
    


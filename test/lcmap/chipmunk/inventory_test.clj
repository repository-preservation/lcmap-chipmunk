(ns lcmap.chipmunk.inventory-test
  (:require [clojure.test :refer :all]
            [lcmap.chipmunk.fixtures :as fixtures]
            [lcmap.chipmunk.inventory :refer :all]))


(use-fixtures :once fixtures/all-fixtures)


#_(deftest lookup-test
  (testing "decoding a source's chips"
    (is )
    #_"the chips have been decoded"))

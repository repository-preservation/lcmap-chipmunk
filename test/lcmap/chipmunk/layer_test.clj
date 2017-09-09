(ns lcmap.chipmunk.layer-test
  (:require [clojure.test :refer :all]
            [lcmap.chipmunk.shared :as shared]
            [lcmap.chipmunk.layer :as layer]
            [lcmap.chipmunk.inventory :as inv]))


(use-fixtures :each shared/mount-fixture
                    shared/layer-fixture
                    shared/layer-data-fixture)


(deftest find-test
  (testing "get the data"
    (let [chips (layer/find! "test_layer" {"x" "1526415" "y" "1922805"})]
      (is (= 1 (count chips)))
      chips)))

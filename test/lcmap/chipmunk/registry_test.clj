(ns lcmap.chipmunk.registry-test
  (:require [clojure.test :refer :all]
            [lcmap.chipmunk.shared :as shared]
            [lcmap.chipmunk.registry :refer :all]))


(use-fixtures :once shared/mount-fixture)


(deftest add-test
  (testing "adding a layer"
    (let [layer {:name "test_layer_b"
                 :tags ["test","layer","bravo"]}]
      (lcmap.chipmunk.registry/add! layer))))

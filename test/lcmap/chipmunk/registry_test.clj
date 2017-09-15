(ns lcmap.chipmunk.registry-test
  (:require [clojure.test :refer :all]
            [lcmap.chipmunk.shared :as shared]
            [lcmap.chipmunk.fixtures :as fixtures]
            [lcmap.chipmunk.registry :refer :all]))


(use-fixtures :once fixtures/all-fixtures)


(deftest add-remove-test
  (testing "layer"
    (testing "add"
      (let [layer {:name "LC08_SRBX" :tags ["LC08" "SRBX"]}]
        (is (= layer (lcmap.chipmunk.registry/add! layer)))))
    (testing "remove an existing layer"
      (is (= true (lcmap.chipmunk.registry/remove! "LC08_SRBX"))))
    (testing "remove a non-existing layer"
      (is (= false (lcmap.chipmunk.registry/remove! "LC08_DOES_NOT_EXIST"))))))

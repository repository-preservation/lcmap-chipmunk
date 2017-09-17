(ns lcmap.chipmunk.registry-test
  (:require [clojure.test :refer :all]
            [camel-snake-kebab.core :as csk]
            [lcmap.chipmunk.shared :as shared]
            [lcmap.chipmunk.fixtures :as fixtures]
            [lcmap.chipmunk.registry :refer :all]))


(use-fixtures :once fixtures/all-fixtures)


(deftest add-remove-test
  (testing "layer"
    (testing "add"
      (let [layer {:name "LC08_SRBX" :tags ["LC08" "SRBX"]}]
        (is (= layer (add! layer)))))
    (testing "lookup an existing layer using varying names"
      (is (= "LC08_SRBX" (-> "LC08_SRBX" lookup! :name)))
      (is (= "LC08_SRBX" (-> "LC08_srbx" lookup! :name)))
      (is (= "LC08_SRBX" (-> "lc08-srbx" lookup! :name))))
    (testing "remove an existing layer"
      (is (= true (remove! "LC08_SRBX"))))
    (testing "remove a non-existing layer"
      (is (= false (remove! "LC08_DOES_NOT_EXIST"))))
    (testing "lookup a non-existing layer"
      (is (= nil (lookup! "lc08_does_not_exist"))))))



(deftest canoncial-layer-name-test
  (testing "LC08_SRBX"
    (is (= :LC08_SRBX (canonical-layer-name :LC08_SRBX)))
    (is (= :LC08_SRBX (canonical-layer-name :lc08_srbx)))
    (is (= :LC08_SRBX (canonical-layer-name :LC08-SRBX)))
    (is (= "LC08_SRBX" (canonical-layer-name "LC08_SRBX")))
    (is (= "LC08_SRBX" (canonical-layer-name "lc08_srbx")))
    (is (= "LC08_SRBX" (canonical-layer-name "LC08-SRBX")))))

(ns lcmap.chipmunk.layer-test
  (:require [clojure.test :refer :all]
            [lcmap.chipmunk.core :as core]
            [lcmap.chipmunk.shared :as shared]
            [lcmap.chipmunk.layer :as layer]
            [lcmap.chipmunk.inventory :as inv]))


(use-fixtures :once shared/mount-fixture
                    shared/layer-fixture
                    shared/layer-data-fixture)


(deftest find-test
  ;; This is an integration test. We're checking the actual values of
  ;; specific chips.
  (testing "get the data"
    (let [chips (layer/lookup! "test_layer" {:x "1502415" :y "1946805"})
          hash  "936f89d12f25727ab7a60f3d9c2cd608"]
      (is (= 1 (count chips)))
      (is (= hash (-> chips first :hash))))))

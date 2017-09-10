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
          hash  "f5aafc702f24102e4d8bf2fc8bf70efe"]
      (is (= 1 (count chips)))
      (is (= hash (-> chips first :hash))))))

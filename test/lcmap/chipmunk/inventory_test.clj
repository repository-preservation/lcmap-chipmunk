(ns lcmap.chipmunk.inventory-test
  (:require [clojure.test :refer :all]
            [lcmap.chipmunk.fixtures :as fixtures]
            [lcmap.chipmunk.shared :as shared]
            [lcmap.chipmunk.inventory :as inventory]
            [lcmap.chipmunk.core :as core]))


(use-fixtures :once fixtures/all-fixtures)


(deftest identify-test
  (testing "generate an ID for a URL"
    (let [url (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif")
          actual (inventory/identify url)
          expected "LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif"]
      (is (= actual expected)))))


(deftest search-test
  (let [url (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif")
        summary (core/ingest url)]
    (testing "search"
      (testing "by tile"
        (let [query {:tile "027009"}
              result (inventory/search query)]
          (is (= 1 (count result)))))
      (testing "by layer"
        (let [query {:layer "LC08_SRB1"}
              result (inventory/search query)]
          (is (= 1 (count result)))))
      (testing "by source id"
        (let [query {:source "LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif"}
              result (inventory/search query)]
          (is (= 1 (count result)))))
      (testing "by url"
        (let [query {:url "http://guest:guest@localhost:9080/LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif"}
              result (inventory/search query)]
          (is (= 1 (count result))))))))

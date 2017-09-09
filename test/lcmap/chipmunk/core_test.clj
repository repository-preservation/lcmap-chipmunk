(ns lcmap.chipmunk.core-test
  (:require [clojure.test :refer :all]
            [lcmap.chipmunk.core :refer :all]
            [lcmap.chipmunk.shared :as shared]))


(use-fixtures :once shared/mount-fixture shared/layer-fixture)


(defn gdal-file-path [path]
  (format "http://guest:guest@localhost:9080/%s" path))


(deftest chip-seq-test
  (testing "INT16 data"
    (let [path (gdal-file-path "LC08_CU_027009_20130701_20170430_C01_V01_SR.tar/LC08_CU_027009_20130701_20170430_C01_V01_SRB2.tif")
          chips (chip-seq path {:layer "test_layer" :source "test_source"})]
      (is (= 2500 (count chips)))
      (is (=  663 (count (filter #(= "fefd82bcde07e5407f77a990eb18d85d" %) (map :hash chips)))))
      (is (= 1838 (count (distinct (map :hash chips)))))))
  (testing "UNIT16 data"
    (let [path (gdal-file-path "LC08_CU_027009_20130701_20170430_C01_V01_SR.tar/LC08_CU_027009_20130701_20170430_C01_V01_PIXELQA.tif")
          chips (chip-seq path {:layer "test_layer" :source "test_source"})]
      (is (= 2500 (count chips)))
      (is (=  663 (count (filter #(= "2fa894fc78e7dd8ef803d35ac129ef81" %) (map :hash chips)))))
      (is (=  172 (count (distinct (map :hash chips)))))))
  (testing "BYTE data"
    (let [path (gdal-file-path "LC08_CU_027009_20130701_20170430_C01_V01_SR.tar/LC08_CU_027009_20130701_20170430_C01_V01_LINEAGEQA.tif")
          chips (chip-seq path {:layer "test_layer" :source "test_source"})]
      (is (= 2500 (count chips)))
      (is (=  663 (count (filter #(= "b85d6fb9ef4260dcf1ce0a1b0bff80d3" %) (map :hash chips)))))
      (is (=   54 (count (distinct (map :hash chips))))))))


(deftest ingest-test
  (testing "ingest valid data"
    (let [path (gdal-file-path "LC08_CU_027009_20130701_20170430_C01_V01_SR.tar/LC08_CU_027009_20130701_20170430_C01_V01_SRB2.tif")
          summary (ingest "test_layer" "test_source" path)]
      (is (= 2500 (-> summary :chips count)))
      (is (= "test_layer" (summary :layer)))
      (is (= "test_source" (summary :source)))))
  (testing "ingest with a bad URL"
    (is (thrown? clojure.lang.ExceptionInfo
                 (ingest "layer-id" "source-id" "bad-url")))))

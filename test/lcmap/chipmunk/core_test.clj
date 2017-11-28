(ns lcmap.chipmunk.core-test
  (:require [clojure.test :refer :all]
            [lcmap.chipmunk.core :refer :all]
            [lcmap.chipmunk.shared :as shared]
            [lcmap.chipmunk.fixtures :as fixtures]))


(use-fixtures :once fixtures/all-fixtures)


(deftest chip-seq-test
  (testing "INT16 data"
    (let [path (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif")
          chips (chip-seq path {:layer "LC08_SRB1" :source "LC08_CU_027009_20130701_20170729_C01_V01_SRB1"})]
      (is (= 2500 (count chips)))
      (is (=  663 (count (filter #(= "fefd82bcde07e5407f77a990eb18d85d" %) (map :hash chips)))))
      (is (= 1838 (count (distinct (map :hash chips)))))))
  (testing "UNIT16 data"
    (let [path (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_PIXELQA.tif")
          chips (chip-seq path {:layer "LC08_PIXELQA" :source "LC08_CU_027009_20130701_20170729_C01_V01_PIXELQA"})]
      (is (= 2500 (count chips)))
      (is (=  663 (count (filter #(= "2fa894fc78e7dd8ef803d35ac129ef81" %) (map :hash chips)))))
      (is (=  229 (count (distinct (map :hash chips)))))))
  (testing "BYTE data"
    (let [path (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_LINEAGEQA.tif")
          chips (chip-seq path {:layer "LC08_LINEAGEQA" :source "LC08_CU_027009_20130701_20170729_C01_V01_LINEAGEQA"})]
      (is (= 2500 (count chips)))
      (is (=  663 (count (filter #(= "b85d6fb9ef4260dcf1ce0a1b0bff80d3" %) (map :hash chips)))))
      (is (=   55 (count (distinct (map :hash chips))))))))


(deftest ingest-test
  (testing "ingest valid data works"
    (let [layer  "LC08_SRB1"
          source "LC08_CU_027009_20130701_20170729_C01_V01_SRB1"
          url    (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif")
          actual (ingest layer source url)]
      (is (= 2500   (-> actual :chips count)))
      (is (= layer  (actual :layer)))
      (is (= source (actual :source)))))
  (testing "ingest valid data"
    (let [url    (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif")
          actual (ingest url)]
      (is (= 2500 (-> actual :chips count))))))


(deftest verify-test
  (testing "verify source and layer"
    (let [layer  "LC08_SRB1"
          source "LC08_CU_027009_20130701_20170729_C01_V01_SRB1"
          url    (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif")
          actual (verify layer source url)]
      (is (= actual true))))
  (testing "verify valid data with missing layer fails"
    ;; This should fail because the layer is not defined.
    (let [layer  "LC08_SRBX"
          source "LC08_CU_027009_20130701_20170729_C01_V01_SRB1"
          url    (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif")]
      (is (thrown? clojure.lang.ExceptionInfo (verify layer source url)))))
  (testing "verify valid URL with wrong layer fails"
    ;; This should fail because the layer and source URL have conflicting names.
    (let [layer  "LC08_SRB1"
          source "LC08_CU_027009_20130701_20170729_C01_V01_SRB1"
          url    (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB2.tif")]
      (is (thrown? clojure.lang.ExceptionInfo (verify layer source url)))))
  (testing "verify non-existent URL fails"
    ;; This should fail because the file in the archive doesn't exist.
    (let [layer  "LC08_SRB1"
          source "LC08_CU_027009_20130701_20170729_C01_V01_SRBX"
          url    (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRBX.tif")]
      (is (thrown? clojure.lang.ExceptionInfo (verify layer source url))))))


(deftest derive-info-test
  (testing "Landsat ARD info"
    (let [layer {:re_groups #{"mission" "projection" "tile" "acquired" "produced" "collection" "version" "band" "extension"}
                 :re_pattern "(?x)(?<prefix>.*)(?<mission>LC08)_(?<projection>CU)_(?<tile>[0-9]{6})_(?<acquired>[0-9]{8})_(?<produced>[0-9]{8})_C(?<collection>[0-9]{2})_V(?<version>[0-9]{2})_(?<band>PIXELQA)(?<extension>.*)"}]
      (derive-info "LC08_CU_027009_20130701_20170729_C01_V01_PIXELQA.tif" layer))))


(deftest derive-layer-name-test
  (testing "find the layer (that exists) for a URL"
    (let [url (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif")
          layer (derive-layer-name url)]
      (is (= "LC08_SRB1" layer))))
  (testing "find a layer (that does not exist) for a URL"
    (let [url (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB2.tif")]
      (is (thrown? clojure.lang.ExceptionInfo (derive-layer-name url))))))


(deftest derive-source-id-test
  (testing "generate an ID for a URL"
    (let [url (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif")
          actual (-> url derive-source-id)
          expected "LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif"]
      (is (= actual expected)))))

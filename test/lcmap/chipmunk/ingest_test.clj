(ns lcmap.chipmunk.ingest-test
  (:require [clojure.test :refer :all]
            [clojure.string :as string]
            [lcmap.chipmunk.shared :as shared]
            [lcmap.chipmunk.fixtures :as fixtures]
            [lcmap.chipmunk.ingest :refer :all]))


(use-fixtures :once fixtures/all-fixtures)

;; ## About These Tests
;;
;; These tests make extensive use of actual ARD for testing. We need to be
;; certain that things work with real data, and not some contrived files.
;; If you change the sample data, many of these tests *will* break (by
;; design).
;;
;; Like the ingest namespace, tests are organized by three groups:
;;
;; 1. Inspect: building info about what will be ingested.
;; 2. Extract: getting raw raster data.
;; 3. Persist: saving chips to a layer and a summary to the inventory.
;;


(deftest inspect-test
  (testing "preparing info for a valid source"
    (let [url (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif")
          info (inspect url)]
      (is (some? (:path info)))
      (is (some? (:meta info)))
      (is (some? (:gdal info)))
      (is (some? (:http info)))
      (is (= "LC08_SRB1" (:ubid info)))
      (is (= #inst "2013-07-01T00:00:00.000-00:00" (:acquired info)))
      (is (= "027009" (:tile info)))
      (is (= "LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif" (:name info)))
      (is (= "http://localhost:9080/LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif" (:url info)))))
  (testing "preparing info for an invalid source"
    (let [url (shared/nginx-url "wtf.tar/idk.tif")]
      (is (thrown? clojure.lang.ExceptionInfo (inspect url))))))


(deftest extract-test
  (testing "extacting chips from a valid source"
    (let [url (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif")
          chips (extract url {})]
      (is (= 2500 (count chips))))))


(deftest persist-test
  (testing "ingest a valid source"
    (let [url (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif")
          actual (save url)]
      (is (= 2500 (count (actual :chips))))))
  (testing "ingest an apparently valid, but missing source"
    (let [url (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRBX.tif")]
      (is (thrown? clojure.lang.ExceptionInfo (save url)))))
  (testing "ingest an invalid source"
    (let [url (shared/nginx-url "wtf.tar/idk.tif")]
      (is (thrown? clojure.lang.ExceptionInfo (save url))))))


;; ## Lower Level Tests
;;
;; These are tests for functions that enable inspection (i.e. building info
;; about a source).
;;

(deftest path-info-test
  (testing "build info from path"
    (testing "to a valid source"
      (let [url (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif")
            actual (path-info url)]
        (is (= "LC08"     (:mission actual)))
        (is (= "CU"       (:region actual)))
        (is (= "027009"   (:tile actual)))
        (is (= "20130701" (:acquired actual)))
        (is (= "20170729" (:produced actual)))
        (is (= "01"       (:collection actual)))
        (is (= "01"       (:version actual)))
        (is (= "SRB1"     (:band actual)))
        (is (= ".tif"     (:extension actual)))
        (is (= "http://localhost:9080/LC08_CU_027009_20130701_20170729_C01_V01_SR.tar" (:archive actual)))))
    (testing "to an invalid source"
      (let [url (shared/nginx-url "wtf.tar/idk.tif")]
        (is (thrown? clojure.lang.ExceptionInfo (path-info url)))))))


(deftest http-info-test
  (testing "build info from an HTTP HEAD request"
    (testing "to a valid source"
      (let [url (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif")
            actual (http-info url)
            size (get-in actual [:headers :content-length])
            etag (get-in actual [:headers :etag])]
        (is (= "22083072" size) "size header different; did the sample data change?")
        ;; Checking an etag value is error prone because timestamps for the last
        ;; modification date of the file can change.
        #_(is (clojure.string/includes? etag "59b7d3b9-150f600") "etag header different; did the sample data change?")))
    (testing "to a non-existent source"
      (let [url (shared/nginx-url "LC08_CU_000000_20130701_20170729_C01_V01_SR.tar/LC08_CU_000000_20130701_20170729_C01_V01_SRBX.tif")
            actual (http-info url)]
        (is (= 404 (:status actual)))))))


(deftest gdal-info-test
  (testing "build info from GDAL metadata"
    (testing "for a valid source"
      (let [url (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif")
            actual (gdal-info url)
            proj_wkt (string/replace (:projection actual) "\"" "")]
        (is (= url (:path actual)))
        (is (= [1484415.0 30.0 0.0 1964805.0 0.0 -30.0] (:geo-transform actual)))
        (is (= 1 (:raster-count actual)))
        (is (= 5000 (:raster-x-size actual)))
        (is (= 5000 (:raster-y-size actual)))
        (is (string/includes? proj_wkt "Albers,GEOGCS[WGS 84,DATUM[WGS_1984,SPHEROID[WGS 84,6378140,298.25"))
        (is (string/includes? proj_wkt "AUTHORITY[EPSG,7030]],AUTHORITY[EPSG,6326]],PRIMEM[Greenwich,0],UNIT[degree,0.0174532925199433],AUTHORITY[EPSG,4326]],PROJECTION[Albers_Conic_Equal_Area],PARAMETER[standard_parallel_1,29.5],PARAMETER[standard_parallel_2,45.5],PARAMETER[latitude_of_center,23],PARAMETER[longitude_of_center,-96],PARAMETER[false_easting,0],PARAMETER[false_northing,0],UNIT[metre,1,AUTHORITY[EPSG,9001]]]"))
))
    (testing "for an invalid source"
      (let [url (shared/nginx-url "wtf.tar/idk.tif")]
        (is (thrown? clojure.lang.ExceptionInfo (gdal-info url)))))))


(deftest meta-info-test
  (testing "build info from ARD XML metadata"
    (testing "for a valid source"
      (let [url (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif")
            actual (meta-info url)]
        (is (= actual {:acquired #inst "2013-07-01T00:00:00.000-00:00"
                       :satellite "LANDSAT_8",
                       :tile "027009",
                       :instrument "OLI/TIRS_Combined",
                       :product_id "LC08_CU_027009_20130701_20170729_C01_V01",
                       :region "CU",
                       :tile_v "009",
                       :tile_h "027",
                       :version "01",
                       :collection "01",
                       }))))
    (testing "for an invalid source"
      (let [url (shared/nginx-url "wtf.tar/idk.tif")
            actual (meta-info url)]
        (is (= nil actual))))))


(deftest derive-name-test
  (testing "derive name for valid source"
    (let [url (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif")
          actual (derive-name url)]
      (is (= "LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif" actual))))
  (testing "derive name for invalid source"
    (let [url (shared/nginx-url "wtf.tar/idk.tif")]
      (is (thrown? clojure.lang.ExceptionInfo (derive-name url))))))


(deftest derive-ubid-test
  (testing "derive layer name from valid source"
    (let [url (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif")
          actual (derive-ubid url)]
      (is (= "LC08_SRB1" actual))))
  (testing "derive layer name from invalid source"
    (let [url (shared/nginx-url "wtf.tar/idk.tif")]
      (is (thrown? clojure.lang.ExceptionInfo (derive-ubid url))))))

(deftest derive-aux-ubid-test
  (testing "derive layer name from aux nlcd source"
    (let [url (shared/nginx-url "AUX_CU_NLCD.tar/AUX_CU_027009_19990731_20171030_V01_NLCD.tif")
          actual (derive-ubid url)]
      (is (= "AUX_NLCD" actual))))
  (testing "derive layer name from aux nlcd training source"
    (let [url (shared/nginx-url "AUX_CU_NLCD.tar/AUX_CU_027009_19990731_20171030_V01_NLCD_TRAINING.tif")
          actual (derive-ubid url)]
      (is (= "AUX_NLCD_TRAINING" actual)))))

(ns lcmap.chipmunk.gdal-test
  (:require [clojure.test :refer :all]
            [lcmap.chipmunk.shared :as shared]
            [lcmap.chipmunk.gdal :refer :all]))


(deftest add-vsi-prefix-test
  (testing "add /vsicurl/"
    (let [url "http://localhost:9090/foo.tif"
          path (add-vsi-prefix url)]
      (is (some? (re-seq #"/vsicurl/" path)))
      (is (not (some? (re-seq #"/vsitar/" path))))))
  (testing "add /vsitar/"
    (let [url "http://localhost:9090/foo.tar/bar.tif"
          path (add-vsi-prefix url)]
      (is (some? (re-seq #"/vsicurl/" path)))
      (is (some? (re-seq #"/vsitar/" path))))))


(deftest gdal-info-test
  (testing "getting info for a path"
    (let [path (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif")
          info (dataset-info path)]
      (is (= 5000 (info :raster-x-size)))
      (is (= 5000 (info :raster-y-size)))
      (is (= "band 1 surface reflectance" (get-in info [:metadata "Band_1"]))))))

(ns lcmap.chipmunk.util-test
  (:require [clojure.test :refer :all]
            [lcmap.chipmunk.shared :as shared]
            [lcmap.chipmunk.util :refer :all]
            [org.httpkit.client :as http]))


(deftest re-mapper-test

  (testing "trivial example"
    (let [re #"(?x)
               (?<foo>    [0-9]{4})_
               (?<bar>    [A-Z]{4})
              "
          s "1234_WXYZ"
          rm (re-mapper re [:foo :bar] s)]
      (is (= (:foo rm) "1234"))
      (is (= (:bar rm) "WXYZ"))))

  (testing "real world name"
    (let [re #"(?x) # ignore whitespace and comments in reg-ex definition
               (?<mission>       [(LC08)|(LE07)|(LT05)|(LT04)]{4})_
               (?<projection>    [(CU|AK|HI)]{2})_
               (?<tile>          [0-9]{6})_
               (?<acquired>      [0-9]{8})_
               (?<produced>      [0-9]{8})_
               C(?<collection>   [0-9]{2})_
               V(?<version>      [0-9]{2})_
               (?<band>          [0-9A-Z]+)
               (?<extension>     .*)"
            s "LC08_CU_027009_20130701_20170729_C01_V01_PIXELQA.tif"
            rm (re-mapper re [:mission :band] s)]
      (is (= (:mission rm) "LC08"))
      (is (= (:band rm) "PIXELQA"))
      (is (= (:tile rm) nil #_"because it's not a listed key...")))))


(deftest re-mapper-test
  (testing "More real world data."
    (let [url "http://guest:guest@localhost:9080/LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif"
          groups #{:mission :tile}
          pattern #".*(?x)(?<prefix>.*)(?<mission>LC08)_(?<projection>CU)_(?<tile>[0-9]{6})_(?<acquired>[0-9]{8})_(?<produced>[0-9]{8})_C(?<collection>[0-9]{2})_V(?<version>[0-9]{2})_(?<band>SRB1)(?<extension>.*)"
          values (re-mapper pattern groups url)]
      (is (= "027009" (values :tile)))
      (is (= "LC08" (values :mission))))))


(deftest re-grouper-test
  (testing "is this a better expression of the same function?"
    (let [actual (-> (re-matcher #"(?<foo>[0-9]{4})_(?<bar>[A-Z]{4})" "1234_WXYZ")
                     (re-grouper #{:foo :bar}))]
      (is (= (:foo actual) "1234"))
      (is (= (:bar actual) "WXYZ")))))


(deftest intervalize-test
  (testing "simple interval conversion"
    (let [interval (intervalize "1980-01-01/2020-01-01")]
      (is (instance? org.joda.time.Interval interval)))))


#_(deftest ard-scene-metadata-path-test
  (testing "getting the url to xml"
    (let [path (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif")
          xml-path (ard-metadata-path path)]
      xml-path)))


#_(deftest ard-scene-metadata-map-test
  (testing "getting the relevant data from XML"
    (let [path (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01.xml")
          data (slurp path)
          result (ard-metadata-map data)]
      result)))


#_(deftest ard-scene-metadata-test
  (testing "getting the relevant data from XML"
    (let [path (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01.xml")
          result (ard-metadata path)]
      result)))

(ns lcmap.chipmunk.ard-test
  (:require [clojure.test :refer :all]
            [lcmap.chipmunk.shared :as shared]
            [lcmap.chipmunk.ard :as ard]))


(deftest locate-test
  (testing "the URL for a tar+tif file"
    (let [url (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif")
          exp (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01.xml")
          act (ard/locate url)]
      (is (= exp act))))
  (testing "the URL for a tif file"
    (let [url (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif")
          exp nil
          act (ard/locate url)]
      (is (= exp act))))
  (testing "the URL for a random URL"
    (let [url "http://google.com"
          exp nil
          act (ard/locate url)]
      (is (= exp act)))))


(deftest fetch-test
  (testing "xml does exist"
    (let [url (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01.xml")
          xml (ard/fetch url)]
      (is (some? xml))))
  (testing "xml does not exist"
    (let [url (shared/nginx-url "this-does-not-exist.xml")
          xml (ard/fetch url)]
      (is (nil? (ard/fetch url))))))


(deftest parse-test
  (testing "parsing some ARD XML metadata"
    (let [xml (slurp "test/nginx/data/LC08_CU_027009_20130701_20170729_C01_V01.xml")
          info (ard/parse xml)]
      (is (some? info))))
  (testing "parsing some non-ARD XML"
    (let [xml "<?xml version=\"1.0\" encoding=\"utf-8\"?><foo>Not ARD XML Metadata</foo>"]
      (is (thrown? clojure.lang.ExceptionInfo (ard/parse xml))))))


(deftest get-info-for-test
  (testing "get info for valid source"
    (let [url (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif")
          info (ard/get-info-for url)]
      (is (= (info :tile) "027009"))
      (is (= (info :region) "CU"))
      (is (= 0 (compare (info :acquired) (.toDate (org.joda.time.DateTime/parse "2013-07-01"))))))))

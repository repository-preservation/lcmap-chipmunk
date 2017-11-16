(ns lcmap.chipmunk.http-test
  (:require [clojure.test :refer :all]
            [lcmap.chipmunk.shared :as shared]
            [lcmap.chipmunk.fixtures :as fixtures]
            [lcmap.chipmunk.config :as config]
            [lcmap.chipmunk.http :refer :all]
            [org.httpkit.client :as http]))


(use-fixtures :once fixtures/all-fixtures)


(deftest get-base-url-test
  (testing "it exists, and it's nuts"
    (let [resp (shared/go-fish {:url ""})]
      (is (= 200 (:status resp)))
      (is (= ["Chipmunk. It's nuts!"] (-> resp :body))))))


(deftest get-healthy-test
  (testing "it looks good right now"
    (let [resp (shared/go-fish {:url "/healthy"})]
      (is (= 200 (:status resp))))))


(deftest get-metrics-test
  (testing "it exists, but there isn't much there right now"
    (let [resp (shared/go-fish {:url "/metrics"})]
      (is (= 200 (:status resp))))))


(deftest get-registry-test
  (testing "GET /chip-specs"
    (let [resp (shared/go-fish {:url "/chip-specs"})]
      (is (= 200 (:status resp)))
      (is (< 1 (-> resp :body count))))))


(deftest post-registry-test
  (testing "POST /chip-specs"
    (let [layer {:name "LC08_SRB1" :tags ["LC08" "SRB1" "aerosol"]}
          resp (shared/go-fish {:url "/chip-specs" :method :post :body layer})]
      (is (= 201 (:status resp))))))


(deftest post-source-then-get-results
  (testing "POST /inventory"
    (let [path "/inventory"
          body {:url (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif")}
          resp (shared/go-fish {:url path :method :post :body body})
          result (get-in resp [:body])]
      (is (= 2500 (count (result :chips))))
      (is (= "027009" (result :tile)))
      (is (= "01" (result :collection)))
      (is (= "01" (result :version)))
      (is (= "LC08_SRB1" (result :layer)))
      (is (= "LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif" (result :source)))))
  (testing "GET /inventory for tile"
    (let [path "/inventory"
          tile "027009"
          resp (shared/go-fish {:url path :query-params {:tile tile}})]
      (is (= 200 (:status resp)))
      (is (= 1 (-> resp :body count)))))
  (testing "GET /inventory for layer"
    (let [path  "/inventory"
          query {"layer" "LC08_SRB1"}
          resp  (shared/go-fish {:url path :query-params query})]
      (is (= 200 (:status resp)))
      (is (= 1 (-> resp :body count)))))
  (testing "GET /inventory for source"
    (let [path   "/inventory"
          source "LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif"
          query  {"source" source}
          resp   (shared/go-fish {:url path :query-params query})]
      (is (= 200 (:status resp)))
      (is (= 1   (-> resp :body count)))))
  (testing "GET /chips that were ingested"
    (let [path  "/chips"
          query {"ubid" "LC08_SRB1" "x" "1526415" "y" "1946805" "acquired" "1980/2020"}
          resp  (shared/go-fish {:url "/chips" :query-params query})]
      (is (= (-> resp :body count) 1))
      (is (= (-> resp :body first :hash) "42eaf57aaf20aac1ae04f539816614ae")))))

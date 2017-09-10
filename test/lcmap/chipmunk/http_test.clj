(ns lcmap.chipmunk.http-test
  (:require [clojure.test :refer :all]
            [lcmap.chipmunk.shared :as shared]
            [lcmap.chipmunk.config :as config]
            [lcmap.chipmunk.layer  :as layer]
            [lcmap.chipmunk.http :refer :all]
            [org.httpkit.client :as http]
            [cheshire.core :as json]))


(use-fixtures :once shared/mount-fixture shared/layer-fixture)


(deftest get-base-url-test
  (testing "it exists, and it's nuts"
    (let [resp (shared/go-fish {:url ""})]
      (is (= 200 (:status resp)))
      (is (= "Chipmunk. It's nuts!" (-> resp :body :result))))))


(deftest get-healthy-test
  (testing "it looks good right now"
    (let [resp (shared/go-fish {:url "/healthy"})]
      (is (= 200 (:status resp))))))


(deftest get-metrics-test
  (testing "it exists, but there isn't much there right now"
    (let [resp (shared/go-fish {:url "/metrics"})]
      (is (= 200 (:status resp))))))


(deftest get-inventory-test
  (testing "GET /inventory isnt' done yet, but it's there."
    (let [resp (shared/go-fish {:url "/inventory"})]
      (is (= 501 (:status resp))))))


(deftest get-layer-test
  (testing "GET /test_layer"
    (let [resp (shared/go-fish {:url "/test_layer"})]
      (is (= 200 (:status resp)))
      (is (= 1 (-> resp :body :result count))))))


(deftest get-registry-test
  (testing "GET /registry isnt' done yet, but it's there."
    (let [resp (shared/go-fish {:url "/registry"})]
      (is (= 200 (:status resp))))))


(deftest post-registry-test
  (testing "POST /registry"
    (let [layer {:layer-id "test_layer_b" :tags ["test" "layer" "bravo"]}
          resp (shared/go-fish {:url "/registry" :method :post :body layer})]
      (is (= 201 (:status resp))))))


(deftest put-layer-test
  (testing "PUT /test_layer_b is not supported"
    (let [layer {:tags ["test" "layer" "bravo"]}
          resp (shared/go-fish {:url "/test_layer_b" :method :put :body layer})]
      (is (= 501 (:status resp))))))


(deftest delete-layer-test
  (testing "DELETE /test_layer_b is not supported"
    (let [resp (shared/go-fish {:url "/test_layer_b" :method :put})]
      (is (= 501 (:status resp))))))


(deftest get-layer-source-test
  (testing "GET /test_layer/test_source"
    (let [resp (shared/go-fish {:url "/test_layer/test_source"})]
      (is (= 200 (:status resp))))))


(deftest put-source-test
  (testing "PUT a valid source"
    (let [body {:url "http://guest:guest@localhost:9080/LC08_CU_027009_20130701_20170430_C01_V01_SR.tar/LC08_CU_027009_20130701_20170430_C01_V01_SRB2.tif"}
          resp (shared/go-fish {:url "/test_layer/test_source"
                                :method :put
                                :body body})]
      (is (= 2500 (count (get-in resp [:body :result :chips]))))
      (is (= "test_layer" (get-in resp [:body :result :layer])))
      (is (= "test_source" (get-in resp [:body :result :source]))))))


(deftest get-chip-test
  (testing "GET chips"
    (let [query {"x" "1526415" "y" "1922805"}
          resp  (shared/go-fish {:url "/test_layer/chips"
                                 :query-params query})]
      (is (= 1 (count (get-in resp [:body :result])))))))

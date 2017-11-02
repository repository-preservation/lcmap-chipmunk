(ns lcmap.chipmunk.http-test
  (:require [clojure.test :refer :all]
            [lcmap.chipmunk.shared :as shared]
            [lcmap.chipmunk.fixtures :as fixtures]
            [lcmap.chipmunk.config :as config]
            [lcmap.chipmunk.layer  :as layer]
            [lcmap.chipmunk.http :refer :all]
            [org.httpkit.client :as http]
            [cheshire.core :as json]))


(use-fixtures :once fixtures/all-fixtures)


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
  (testing "GET /inventory without params"
    (let [resp (shared/go-fish {:url "/inventory"})]
      (is (= 400 (:status resp)))
      (is (= 0 (-> resp :body :result count)))))
  (testing "GET /inventory for tile without sources"
    (let [resp (shared/go-fish {:url "/inventory" :query-params {:tile "001001"}})]
      (is (= 200 (:status resp)))
      (is (= 0 (-> resp :body :result count))))))


(deftest get-layer-test
  ;; Just a reminder, this test relies on fixture data...
  (testing "GET /LC08_SRB1"
    (let [resp (shared/go-fish {:url "/LC08_SRB1"})]
      (is (= 200 (:status resp)))
      (is (= {:name "LC08_SRB1"} (-> resp :body :result (select-keys [:name])))))
    (let [resp (shared/go-fish {:url "/lc08_srb1"})]
      (is (= 200 (:status resp)))
      (is (= {:name "LC08_SRB1"} (-> resp :body :result (select-keys [:name])))))
    (let [resp (shared/go-fish {:url "/LC08-srb1"})]
      (is (= 200 (:status resp)))
      (is (= {:name "LC08_SRB1"} (-> resp :body :result (select-keys [:name])))))))


(deftest get-registry-test
  (testing "GET /registry isnt' done yet, but it's there."
    (let [resp (shared/go-fish {:url "/registry"})]
      (is (= 200 (:status resp))))))


(deftest post-registry-test
  (testing "POST /registry"
    (let [layer {:name "LC08_SRB1" :tags ["LC08" "SRB1" "aerosol"]}
          resp (shared/go-fish {:url "/registry" :method :post :body layer})]
      (is (= 201 (:status resp))))))


(deftest put-layer-test
  (testing "PUT layer is not supported"
    (let [layer {:tags ["LC08" "SRBX"]}
          resp (shared/go-fish {:url "/LC08_SRBX" :method :put :body layer})]
      (is (= 501 (:status resp))))))


(deftest delete-layer-test
  (testing "DELETE layer is not supported"
    (let [resp (shared/go-fish {:url "/LC08_SRB1" :method :delete})]
      (is (= 501 (:status resp))))))


(deftest post-source-then-get-results
  ;; These tests are combined because the source and chips will not exist until
  ;; they have been ingested first. This is a slow test because it tests the
  ;; core functionality of the app: ingest data and make it available as chips.
  (testing "ingest"
    (testing "via HTTP POST"
      (let [path "inventory"
            body {:url (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif")}
            resp (shared/go-fish {:url path :method :post :body body})]
        (is (= 2500 (count (get-in resp [:body :result :chips]))))
        (is (= "027009" (get-in resp [:body :result :tile])))
        (is (= "01" (get-in resp [:body :result :collection])))
        (is (= "01" (get-in resp [:body :result :version])))
        (is (= "LC08_SRB1" (get-in resp [:body :result :layer])))
        (is (= "LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif"
               (get-in resp [:body :result :source])))))
    (testing "then GET source"
      (let [source "LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif"
            layer  "LC08_SRB1"
            resp   (shared/go-fish {:url "/inventory" :method :get :query-params {:layer layer :source source}})
            result (-> resp :body :result first)]
        (is (= "LC08_SRB1" (result :layer)))
        (is (= "LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif" (result :source)))))
    (testing "then GET inventory for tile"
      (let [resp (shared/go-fish {:url "/inventory" :query-params {:tile "027009"}})]
        (is (= 200 (:status resp)))
        (is (= 1 (-> resp :body :result count)))))
    (testing "then GET layer data"
      (let [resp (shared/go-fish {:url "/LC08_SRB1/chips" :query-params {"x" "1526415" "y" "1946805"}})]
        (is (= (-> resp (get-in [:body :result]) count) 1))
        (is (= (-> resp :body :result first :hash) "42eaf57aaf20aac1ae04f539816614ae")))
      (let [resp (shared/go-fish {:url "/chips" :query-params {"layer" "LC08_SRB1" "x" "1526415" "y" "1946805"}})]
        (is (= (-> resp (get-in [:body :result]) count) 1))))))


(deftest put-source-in-wrong-layer-test
  (testing "prevent cross contamination"
    (let [path "LC08_SRB1/LC08_CU_027009_20130701_20170729_C01_V01_SRB1"
          body {:url (shared/nginx-url "LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_PIXELQA.tif")}
          resp (shared/go-fish {:url path :method :put :body body})]
      (is (some? (get-in resp [:body :errors])))
      (is (empty? (get-in resp [:body :result]))))))

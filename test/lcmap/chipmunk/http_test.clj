(ns lcmap.chipmunk.http-test
  (:require [clojure.test :refer :all]
            [lcmap.chipmunk.shared :as shared]
            [lcmap.chipmunk.config :as config]
            [lcmap.chipmunk.layer  :as layer]
            [lcmap.chipmunk.http :refer :all]
            [org.httpkit.client :as client]
            [cheshire.core :as json]))


(use-fixtures :once shared/mount-fixture shared/layer-fixture)

;;
;; Dear Future Self,
;;
;; This is one of the most important tests in the entire suite
;; because it fully exercises ingest from end-to-end. Don't
;; try to make it faster by stubbing or mocking behavior.
;;
;; Love,
;; - me.
;;

(deftest put-source-test
  ;; This layer must exist (one is created as a fixture).
  (testing "PUT a valid source"
    (let [layer-id    "test_layer"
          source-id   "LC08_CU_027009_20130701_20170430_C01_V01_SRB2"
          source-url  "http://guest:guest@localhost:9080/LC08_CU_027009_20130701_20170430_C01_V01_SR.tar/LC08_CU_027009_20130701_20170430_C01_V01_SRB2.tif"
          resource    (format "layers/%s/source/%s" layer-id source-id)
          resp    (-> {:url resource :method :put :body {:url source-url}} shared/pew client/request)
          body    (-> @resp :body (json/decode keyword))]
      (is (= 2500 (count (body :results))))))
  ;; Obviously, this layer must not exist.
  (testing "PUT in an invalid layer"
    #_"Layer does not exist")
  (testing "PUT an invalid source"
    #_"Data does not exist"
    #_"Data isn't a raster"
    #_"Data is a raster but doesn't conform layer definition"))

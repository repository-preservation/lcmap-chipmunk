(ns lcmap.chipmunk.http
  "Handlers for HTTP requests."
  (:require [clojure.tools.logging :as log]
            [cheshire.core :as json]
            [cheshire.generate :as json-gen :refer [add-encoder]]
            [compojure.core :as compojure]
            [metrics.ring.expose]
            [mount.core :as mount]
            [org.httpkit.server :as server]
            [ring.middleware.json :as ring-json]
            [ring.middleware.defaults :as ring-defaults]
            [ring.middleware.keyword-params :as ring-keyword-params]
            [ring.middleware.cors :as ring-cors]
            [lcmap.chipmunk.config :as config]
            [lcmap.chipmunk.registry :as registry]
            [lcmap.chipmunk.inventory :as inventory]
            [lcmap.chipmunk.chips :as chips]
            [lcmap.chipmunk.experimental :as experimental]
            [lcmap.chipmunk.core :as core])
  (:import [org.joda.time DateTime]
           [org.apache.commons.codec.binary Base64]))


(defn get-base
  "Build response for base URL."
  [req]
  {:status 200 :body ["Chipmunk. It's nuts!"]})


(defn get-chip-specs
  "Get all chips (in a layer) specified meeting criteria in params."
  [{:keys [params] :as req}]
  (log/debugf "GET chip-specs '%s'" params)
  (let [results (registry/search! params)]
    {:status 200 :body (map #(assoc % :ubid (get % :name)) results)}))


(defn post-chip-specs
  "Create (or update) a layer's properties."
  [{:keys [body] :as req}]
  (log/debugf "POST chip-specs '%s'" body)
  (let [layer (registry/add! body)]
    {:status 201 :body layer}))


(defn get-chips
  "Get all chips (in a layer) specified meeting criteria in params."
  [{:keys [params] :as req}]
  (log/debugf "GET chips '%s'" params)
  (let [ubid  (get-in req [:params :ubid])
        results (map #(assoc % :ubid ubid) (chips/search! params))]
    {:status 200 :body results}))


(defn get-sources
  "Find sources matching query params."
  [{:keys [params] :as req}]
  (log/debugf "GET sources '%s'" params)
  {:status 200 :body (inventory/search params)})


(defn post-source
  "Ingest data specified by source into implicit layer."
  [{:keys [:body] :as req}]
  (log/debugf "POST source '%s'" (:url body))
  {:status 200 :body (core/ingest (:url body))})


(defn healthy
  "Handler for checking application health."
  [request]
  (log/debug "GET health")
  {:status 200 :body {:healthy true}})


(defn metrics
  "Handler for reporting metrics."
  [request]
  (log/debug "GET metrics")
  (metrics.ring.expose/serve-metrics {}))


(defn snap-point
  ""
  [{:keys [params] :as request}]
  (log/debug "GET snap")
  (let [layer (registry/lookup! (-> request :params :ubid))
        p1 (chips/snap params layer)
        p2 (experimental/snap-matrix params layer)]
    {:status 200 :body {:snap-legacy p1 :snap-matrix p2}}))


(compojure/defroutes routes
  (compojure/context "/" request
    (compojure/GET "/" []
      (get-base request))
    (compojure/GET "/chips" []
      (get-chips request))
    (compojure/GET "/chip-specs" []
      (get-chip-specs request))
    (compojure/POST "/chip-specs" []
      (post-chip-specs request))
    (compojure/GET  "/snap" []
      (snap-point request))
    (compojure/GET "/inventory" []
      (get-sources request))
    (compojure/POST "/inventory" []
      (post-source request))
    (compojure/GET "/healthy" []
      (healthy request))
    (compojure/GET "/metrics" []
      (metrics request))))


(defn wrap-exception-handling
  "Catch otherwise unhandled exceptions."
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch java.lang.RuntimeException cause
        (log/errorf cause "middleware caught exception: %s" (.getMessage cause))
        {:status 500 :body (json/encode {:error (.getMessage cause) :problem (ex-data cause)})}))))


(def app
  (-> routes
      (ring-json/wrap-json-body {:keywords? true})
      (ring-json/wrap-json-response)
      (ring-cors/wrap-cors #".*")
      (ring-defaults/wrap-defaults ring-defaults/api-defaults)
      (ring-keyword-params/wrap-keyword-params)
      (wrap-exception-handling)))


(defn iso8601-encoder
  "Transform a Joda DateTime object into an ISO8601 string."
  [date-time generator]
  (log/debug "encoding DateTime to ISO8601")
  (.writeString generator (str date-time)))


(defn base64-encoder
  "Base64 encode a byte-buffer, usually raster data from Cassandra."
  [buffer generator]
  (log/debug "encoding HeapByteBuffer")
  (let [size (- (.limit buffer) (.position buffer))
        copy (byte-array size)]
    (.get buffer copy)
    (.writeString generator (Base64/encodeBase64String copy))))


(mount/defstate json-encoders
  :start (do
           (json-gen/add-encoder org.joda.time.DateTime iso8601-encoder)
           (json-gen/add-encoder java.nio.HeapByteBuffer base64-encoder)))


(declare http-server)


(defn http-start []
  (let [port (-> config/config :http-port Integer/parseInt)]
    (log/debugf "start http server on port %s" port)
    (server/run-server #'app {:port port})))


(defn http-stop []
  (log/debug "stop http server")
  (http-server))


(mount/defstate http-server
  :start (http-start)
  :stop  (http-stop))

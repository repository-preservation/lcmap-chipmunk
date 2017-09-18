(ns lcmap.chipmunk.http
  "Handlers for HTTP requests."
  (:require [clojure.tools.logging :as log]
            [clojure.stacktrace :as stacktrace]
            [cheshire.core :as json]
            [cheshire.generate :as json-gen :refer [add-encoder]]
            [compojure.core :as compojure]
            [metrics.ring.expose]
            [mount.core :as mount]
            [org.httpkit.server :as server]
            [ring.middleware.json :as ring-json]
            [ring.middleware.defaults :as ring-defaults]
            [ring.middleware.keyword-params :as ring-keyword-params]
            [ring.util.response :as ring-response]
            [lcmap.chipmunk.config :as config]
            [lcmap.chipmunk.registry :as registry]
            [lcmap.chipmunk.inventory :as inventory]
            [lcmap.chipmunk.layer :as layer]
            [lcmap.chipmunk.core :as core])
  (:import [org.joda.time.DateTime]
           [org.apache.commons.codec.binary Base64]))


(defn get-registry
  "Get all registered layers."
  [req]
  (log/debugf "GET layers")
  (if-let [layers (registry/all!)]
    {:status 200 :body {:result layers}}))


(defn get-layer
  "Get layer by name."
  [layer-name req]
  (log/debugf "GET layer %s" layer-name)
  {:status 200 :body {:result (registry/lookup! layer-name)}})


(defn get-chips
  "Get all chips in layer specified meeting criteria in params."
  [layer-id req]
  (log/debugf "GET layer %s chips" layer-id)
  (let [chips (layer/lookup! layer-id (:params req))]
    {:status 200 :body {:result chips}}))


(defn post-registry
  "Register a layer."
  [req]
  (let [layer (req :body)]
    (log/debugf "POST registry %s" layer)
    {:status 201 :body {:result (registry/add! layer)}}))


(defn get-source
  "Get source metadata."
  [layer-id source-id req]
  (log/debugf "GET source %s in layer %s" source-id layer-id)
  (if-let [source (first (inventory/lookup! layer-id source-id))]
    {:status 200 :body {:result source}}
    {:status 404 :body {:result nil}}))


(defn put-source
  "Create and ingest data specified by source."
  [layer-id source-id {{url :url} :body}]
  (log/debugf "PUT source '%s' at URL '%s' into layer '%s'" source-id url layer-id)
  (if-let [source (core/ingest layer-id source-id url)]
    {:status 200 :body {:result source}}
    {:status 500 :body {:errors ["could not handle source"]}}))


(defn healthy
  "Handler for checking application health."
  [request]
  (log/debug "get health")
  (if true
    {:status 200 :body {:healthy true}}
    {:status 500 :body {:healthy false}}))


(defn metrics
  "Handler for reporting metrics."
  [request]
  (log/debug "get metrics")
  (metrics.ring.expose/serve-metrics {}))


(defn unsupported
  "Explain why a method is not suppoted by a resource."
  [reason]
  {:status 501 :body {:result reason}})


(compojure/defroutes routes
  (compojure/context "/" request
    (compojure/GET "/" []
      {:status 200 :body {:result "Chipmunk. It's nuts!"}})
    (compojure/GET "/healthy" []
      (healthy request))
    (compojure/GET "/metrics" []
      (metrics request))
    (compojure/GET "/registry" []
      (get-registry request))
    (compojure/POST "/registry" []
      (post-registry request))
    (compojure/GET "/inventory" []
      (unsupported "Browsing and searching inventory not supported at this time."))
    (compojure/GET "/:layer-id" [layer-id]
      (get-layer layer-id request))
    (compojure/PUT "/:layer-id" [layer-id]
      (unsupported "User POST /registry instead."))
    (compojure/DELETE "/:layer-id" [layer-id]
      (unsupported "Removing layers not supported via HTTP at this time."))
    (compojure/GET "/:layer-id/chips" [layer-id]
      (get-chips layer-id request))
    (compojure/GET "/:layer-id/:source-id" [layer-id source-id]
      (get-source layer-id source-id request))
    (compojure/PUT "/:layer-id/:source-id" [layer-id source-id]
      (put-source layer-id source-id request))))


(defn wrap-exception-handling
  "Catch otherwise unhandled exceptions."
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch java.lang.RuntimeException cause
        (log/error cause "middleware caught exception: %s")
        {:status 500 :body (json/encode {:errors (.getMessage cause)})}))))


(def app
  (-> routes
      (ring-json/wrap-json-body {:keywords? true})
      (ring-json/wrap-json-response)
      (ring-defaults/wrap-defaults ring-defaults/api-defaults)
      (ring-keyword-params/wrap-keyword-params)
      (wrap-exception-handling)))


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


(defn iso8601-encoder
  "Transform a Joda DateTime object into an ISO8601 string."
  [date-time generator]
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

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
            [lcmap.chipmunk.core :as core])
  (:import [org.joda.time DateTime]
           [org.apache.commons.codec.binary Base64]))


(defn get-registry
  "Get all registered layers."
  [req]
  (log/debugf "GET layers")
  (if-let [layers (registry/all!)]
    {:status 200 :body {:result layers}}))


(defn get-chips
  "Get all chips (in a layer) specified meeting criteria in params."
  [{:keys [params] :as req}]
  ;; FYI: To maintain compatability with previous chip providing resources,
  ;; each chip has a ubid property set.
  (log/debugf "GET chips %s" params)
  (let [ubid (get-in req [:params :ubid])
        results (chips/lookup! params)
        results+ (map #(assoc % :ubid ubid) results)]
    {:status 200 :body results+}))


(defn get-chip-specs
  "Get all chips (in a layer) specified meeting criteria in params."
  [{:keys [params] :as req}]
  ;; FYI: Unlike other resources, the chips-spec resource does not
  ;; associate the results with a ':results' key because existing clients
  ;; do not expect this convention.
  (log/debugf "GET chip-specs %s" params)
  (let [results (registry/search! params)
        results+ (map #(assoc % :ubid (get % :name)) results)]
    {:status 200 :body results+}))


(defn post-registry
  "Create (or update) a layer's properties."
  [req]
  (let [layer (req :body)]
    (log/debugf "POST registry %s" layer)
    {:status 201 :body {:result (registry/add! layer)}}))


(defn get-sources
  "Find sources matching query params."
  [{:keys [params] :as req}]
  (log/debugf "GET sources %s" params)
  (if (seq params)
    {:status 200 :body {:result (inventory/search params)}}
    {:status 400 :body {:result []}}))


(defn post-source
  "Ingest data specified by source into implicit layer."
  [{:keys [:body] :as req}]
  (log/debugf "POST source '%s'" (:url body))
  (if-let [source (core/ingest (:url body))]
    {:status 200 :body {:result source}}
    {:status 500 :body {:errors ["could not handle source"]}}))


(defn healthy
  "Handler for checking application health."
  [request]
  (log/debug "GET health")
  (if true
    {:status 200 :body {:healthy true}}
    {:status 500 :body {:healthy false}}))


(defn metrics
  "Handler for reporting metrics."
  [request]
  (log/debug "GET metrics")
  (metrics.ring.expose/serve-metrics {}))


(compojure/defroutes routes
  (compojure/context "/" request
    (compojure/GET "/" []
      {:status 200 :body {:result "Chipmunk. It's nuts!"}})
    (compojure/GET "/registry" []
      (get-registry request))
    (compojure/POST "/registry" []
      (post-registry request))
    (compojure/GET "/inventory" []
      (get-sources request))
    (compojure/POST "/inventory" []
      (post-source request))
    (compojure/GET "/chips" []
      (get-chips request))
    (compojure/GET "/chip-specs" []
      (get-chip-specs request))
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
        {:status 500 :body (json/encode {:errors (.getMessage cause)})}))))


(def app
  (-> routes
      (ring-json/wrap-json-body {:keywords? true})
      (ring-json/wrap-json-response)
      (ring-cors/wrap-cors #".*")
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

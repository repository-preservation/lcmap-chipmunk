(ns lcmap.chipmunk.http
  "Handlers for HTTP requests."
  (:require [clojure.tools.logging :as log]
            [clojure.stacktrace :as stacktrace]
            [cheshire.core :as json]
            [cheshire.generate :as json-gen :refer [add-encoder]]
            [compojure.core :as compojure]
            [mount.core :as mount]
            [org.httpkit.server :as server]
            [ring.middleware.json :as ring-json]
            [ring.middleware.defaults :as ring-defaults]
            [ring.middleware.keyword-params :as ring-keyword-params]
            [ring.middleware.cors :as ring-cors]
            [lcmap.chipmunk.config :as config]
            [lcmap.chipmunk.ingest :as ingest]
            [lcmap.chipmunk.registry :as registry]
            [lcmap.chipmunk.inventory :as inventory]
            [lcmap.chipmunk.chips :as chips]
            [lcmap.chipmunk.grid :as grid]
            [lcmap.chipmunk.core :as core])
  (:import [org.joda.time DateTime]
           [org.apache.commons.codec.binary Base64]
           [com.fasterxml.jackson.core.JsonGenerator]))

(set! *warn-on-reflection* true)

;; # Overview
;;
;; This namespace provides functions that produce responses
;; to requests for various resources. It also defines routes,
;; middleware, encoders, and an HTTP server state.
;;


;; # Responders
;;
;; These functions are defined as such to keep routes concise.
;; Each produces a Ring-style response map. You may notice some
;; logic that 'renames' parameters; this was done to maintain
;; compatability with previous consumers of a similar REST API.
;;
;; In order to avoid duplication, resources that provide the same
;; behavior without changing names have been avoided.
;;

(defn get-base
  "Build response for base URL."
  [req]
  {:status 200 :body ["Chipmunk. It's nuts!"]})


(defn get-registry
  "Get all chips (in a layer) specified meeting criteria in params."
  [{:keys [params] :as req}]
  (log/debugf "GET registry '%s'" params)
  (let [results (registry/all!)]
    {:status 200 :body results}))


(defn post-registry
  "Create (or update) a layer's properties."
  [{:keys [body] :as req}]
  (log/debugf "POST registry '%s'" body)
  (let [layer (map registry/add! body)]
    {:status 201 :body layer}))


(defn get-chips
  "Get all chips (in a layer) specified meeting criteria in params."
  [{:keys [params] :as req}]
  (log/debugf "GET chips '%s'" params)
  (let [ubid  (get-in req [:params :ubid])
        results (map #(assoc % :ubid ubid) (chips/search! params))]
    {:status 200 :body results :headers {"etag" (chips/etag results)}}))


(defn get-sources
  "Find sources matching query params."
  [{:keys [params] :as req}]
  (log/debugf "GET sources '%s'" params)
  {:status 200 :body (inventory/search params)})


(defn post-source
  "Ingest data specified by source into implicit layer."
  [{:keys [:body] :as req}]
  (log/debugf "POST source '%s'" (:url body))
  {:status 200 :body (ingest/save (:url body))})


(defn healthy
  "Handler for checking application health."
  [request]
  (log/debug "GET health")
  {:status 200 :body {:healthy true}})


(defn get-grid
  "Obtain parameters for a grid of the given name."
  [{:keys [params] :as request}]
  (log/debug "GET /grid")
  (let [grids (grid/all!) ]
    {:status 200 :body grids}))


(defn post-grid
  "Create (or update) a layer's properties."
  [{:keys [body] :as req}]
  (log/debugf "POST grid '%s'" body)
  (let [grid (map grid/add! body)]
    {:status 201 :body grid}))


(defn get-snap
  "Convert points to those that are 'on' the grid."
  [{:keys [params] :as request}]
  (log/debug "GET /grid/snap")
  (let [grids (grid/all!)
        snaps (into {} (map #(grid/snap params %)) grids)]
    {:status 200 :body snaps}))


(defn get-near
  "Find points near the given point"
  [{:keys [params] :as request}]
  (log/debug "GET /grid/near")
  (let [grids (grid/all!)
        nears (into {} (map #(grid/near params %)) grids)]
    {:status 200 :body nears}))


;; ## Routes
;;
;; As mentioned prior, the route definition does nothing aside
;; from invoke the corresponding function. This keeps routes
;; concise (and readable).
;;

(compojure/defroutes routes
  (compojure/context "/" request
    (compojure/GET "/" []
      (get-base request))
    (compojure/GET "/chips" []
      (get-chips request))
    (compojure/GET "/registry" []
      (get-registry request))
    (compojure/POST "/registry" []
      (post-registry request))
    (compojure/GET "/inventory" []
      (get-sources request))
    (compojure/POST "/inventory" []
      (post-source request))
    (compojure/GET "/grid" []
      (get-grid request))
    (compojure/POST "/grid" []
      (post-grid request))
    (compojure/GET "/grid/snap" []
      (get-snap request))
    (compojure/GET "/grid/near" []
      (get-near request))
    (compojure/GET "/healthy" []
      (healthy request))))


;; ## Middleware
;;
;; The only custom middleware provided by Chipmunk is an exception
;; handler that produces a generic error message.
;;


(defn cause-messages
  "Produce a list of messages for a Throwable's cause stack trace."
  [throwable]
  (->> throwable
       (iterate (fn [^Throwable t] (.getCause t)))
       (take-while some?)
       (map (fn [^Throwable t] (.getMessage t)))
       (into [])))


(defn wrap-exception-handling
  "Catch otherwise unhandled exceptions."
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch java.lang.RuntimeException ex
        (let [messages (cause-messages ex)]
          (log/errorf "exception: %s" messages)
          {:status 500 :body (json/encode {:errors messages})})))))


;; ## Handler
;;
;; This handler combines all routes and middleware. In addition to
;; our own middleware, we include functions that:
;;
;; - convert params map keys into keywords
;; - convert request and and response bodies to JSON
;; - add CORS headers to responses
;;

(def app
  (-> routes
      (ring-json/wrap-json-body {:keywords? true})
      (ring-json/wrap-json-response)
      (ring-defaults/wrap-defaults ring-defaults/api-defaults)
      (ring-keyword-params/wrap-keyword-params)
      (wrap-exception-handling)
      (ring-cors/wrap-cors #".*")))


;; ## Encoders
;;
;; These functions simplify the converstion of values that do
;; not have a default way of producing a serialized string.
;;

(defn iso8601-encoder
  "Transform a Joda DateTime object into an ISO8601 string."
  [^org.joda.time.DateTime date-time ^com.fasterxml.jackson.core.JsonGenerator generator]
  (log/trace "encoding DateTime to ISO8601")
  (.writeString generator (str date-time)))


(defn base64-encoder
  "Base64 encode a byte-buffer, usually raster data from Cassandra."
  [^java.nio.HeapByteBuffer buffer ^com.fasterxml.jackson.core.JsonGenerator generator]
  (log/trace "encoding HeapByteBuffer")
  (let [size (- (.limit buffer) (.position buffer))
        copy (byte-array size)]
    (.get buffer copy)
    (.writeString generator (Base64/encodeBase64String copy))))


(mount/defstate json-encoders
  :start (do
           (json-gen/add-encoder org.joda.time.DateTime iso8601-encoder)
           (json-gen/add-encoder java.nio.HeapByteBuffer base64-encoder)))


;; ## HTTP Server
;;
;; This state starts a web server that uses the `app` handler
;; to process requests.
;;

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

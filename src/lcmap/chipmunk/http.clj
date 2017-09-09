(ns lcmap.chipmunk.http
  "Handlers for HTTP requests."
  (:require [clojure.tools.logging :as log]
            [compojure.core :as compojure]
            [metrics.ring.expose]
            [mount.core :as mount]
            [org.httpkit.server :as server]
            [ring.middleware.json :as ring-json]
            [ring.middleware.defaults :as ring-defaults]
            [ring.util.response :as ring-response]
            [lcmap.chipmunk.config :as config]
            [lcmap.chipmunk.registry :as registry]
            [lcmap.chipmunk.inventory :as inventory]
            [lcmap.chipmunk.layer :as layer]
            [lcmap.chipmunk.core :as core]))


(defn get-registry
  ""
  [req]
  (log/debugf "GET layers")
  (if-let [layers (registry/all!)]
    {:status 200 :body {:result layers}}))


(defn get-layer
  ""
  [layer-id req]
  (log/debugf "GET layer %s" layer-id)
  {:status 200 :body {:result (registry/lookup! layer-id)}})


(defn put-layer
  ""
  [layer-id req]
  (log/debugf "PUT layer %s" layer-id)
  {:status 201 :body {:result (registry/add! layer-id)}})


(defn get-source
  ""
  [layer-id source-id req]
  (log/debug "GET source %s in layer %s" source-id layer-id)
  (if-let [source (inventory/lookup! layer-id source-id)]
    {:status 200 :body {:result source}}
    {:status 404 :body {:result []}}))


(defn put-source
  ""
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


(compojure/defroutes routes
  (compojure/context "/" request
    (compojure/GET "/" []
      {:status 200 :body {:result "Chipmunk. It's nuts!"}})
    (compojure/GET "/healthy" []
      (healthy request))
    (compojure/GET "/metrics" []
      (metrics request))
    (compojure/GET "/registry" []
      {:status 200 :body {:result (registry/all!)}})
    (compojure/GET "/inventory" []
      {:status 501})
    (compojure/GET "/:layer-id" [layer-id]
      (get-layer layer-id request))
    (compojure/PUT "/:layer-id" [layer-id]
      (put-layer layer-id request))
    (compojure/GET "/:layer-id/:source-id" [layer-id source-id]
      (get-source layer-id source-id request))
    (compojure/PUT "/:layer-id/:source-id" [layer-id source-id]
      (put-source layer-id source-id request))))


(defn wrap-exception-handling
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch java.lang.RuntimeException cause
        (log/errorf (.getMessage cause))
        {:status 500 :body {:errors (.getMessage cause)}}))))


(def app
  (-> routes
      (ring-json/wrap-json-body {:keywords? true})
      (ring-json/wrap-json-response)
      (ring-defaults/wrap-defaults ring-defaults/api-defaults)
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

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
            [lcmap.chipmunk.layer :as layer]
            [lcmap.chipmunk.core :as core]))


(defn get-layers
  ""
  [req]
  (log/debugf "GET layers")
  {:body (layer/all!)})


(defn get-layer
  ""
  [layer-id req]
  (log/debug "GET layer %s" layer-id)
  {:body (layer/lookup! layer-id)})


(defn put-layer [layer-id req]
  (log/debug "PUT layer %s" layer-id)
  {:body {:count (layer/create! (keyword layer-id))}})


(defn get-source [layer-id source-id req]
  (log/debug "GET source %s in layer %s" source-id layer-id)
  {:body "get-source"})


(defn put-source
  ""
  [layer-id source-id req]
  (let [url    (get-in req [:body :url])
        result (core/ingest layer-id source-id url)]
    {:status 200 :body {:result result}}))


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
      {:status 200 :body "Chipmunk. It's nuts!"})
    (compojure/GET "/healthy" []
      (healthy request))
    (compojure/GET "/metrics" []
      (metrics request))
    (compojure/GET "/layers" []
      (get-layers request))
    (compojure/GET "/layers/:layer-id" [layer-id]
      (get-layer layer-id request))
    (compojure/PUT "/layers/:layer-id" [layer-id]
      (put-layer layer-id request))
    (compojure/GET "/layers/:layer-id/source/:source-id" [layer-id source-id]
      (get-source layer-id source-id request))
    (compojure/PUT "/layers/:layer-id/source/:source-id" [layer-id source-id]
      (put-source layer-id source-id request))))


(def app
  (-> routes
      (ring-json/wrap-json-body {:keywords? true})
      (ring-json/wrap-json-response)
      (ring-defaults/wrap-defaults ring-defaults/api-defaults)))


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

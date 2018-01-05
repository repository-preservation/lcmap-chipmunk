(defproject chipmunk "1.0.0-RC1"
  :description "Geospatial data ingest tools"
  :url "http://github.com/usgs-eros/lcmap-chipmunk"
  :license {:name "Unlicense"
            :url ""}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.gdal/gdal "1.11.2"]
		 [camel-snake-kebab "0.4.0"]
                 [digest "1.4.6"]
                 [clojurewerkz/buffy "1.1.0"]
                 [org.clojure/tools.logging "0.4.0"]
                 [org.slf4j/slf4j-log4j12 "1.7.21"]
                 [environ "1.1.0"]
                 [cc.qbits/alia-all "4.0.3"]
                 [cc.qbits/hayt "4.0.0"]
                 [cc.qbits/alia-joda-time "4.0.2"]
                 [mount "0.1.11"]
                 [http-kit "2.2.0"]
                 [ring "1.6.2"]
                 [ring/ring-defaults "0.3.1"]
                 [ring/ring-json "0.4.0"]
                 [jumblerg/ring.middleware.cors "1.0.1"]
                 [compojure "1.6.0"]
                 [cheshire "5.8.0"]
                 [metrics-clojure "2.9.0"]
                 [metrics-clojure-health "2.9.0"]
                 [metrics-clojure-jvm "2.9.0"]
                 [metrics-clojure-ring "2.9.0"]
                 [net.mikera/core.matrix "0.61.0"]
                 [net.mikera/vectorz-clj "0.47.0"]
                 [com.github.kyleburton/clj-xpath "1.4.3"]]
  :plugins [[lein-environ "1.1.0"]]
  :profiles {:dev     {:resource-paths ["dev"]}
             :repl    {:resource-paths ["dev"]
		       :dependencies [[cider/cider-nrepl "0.15.1"]]}
             :test    {:resource-paths ["test" "test/resources"]}
             :uberjar {:omit-source true
                       :aot :all}}
  :jvm-opts ["-server"]
  :main lcmap.chipmunk.main
  :repl-options {:init-ns user})

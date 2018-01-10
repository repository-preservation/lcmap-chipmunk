FROM usgseros/ubuntu-gis-clj
MAINTAINER USGS LCMAP http://eros.usgs.gov

ARG version
ENV jarfile chipmunk-$version-standalone.jar
EXPOSE 5656

RUN mkdir /app
WORKDIR /app
COPY target/$jarfile $jarfile
COPY resources/log4j.properties log4j.properties

ENTRYPOINT java -server -Xms$Xms -Xmx$Xmx -XX:+UseG1GC -jar $jarfile

FROM usgseros/ubuntu-gis-clj
MAINTAINER USGS LCMAP http://eros.usgs.gov

ENV version 0.1.0-SNAPSHOT
ENV jarfile chipmunk-$version-standalone.jar
EXPOSE 5678

RUN mkdir /app
WORKDIR /app
COPY target/$jarfile $jarfile
COPY resources/log4j.properties log4j.properties

ENTRYPOINT java -jar $jarfile

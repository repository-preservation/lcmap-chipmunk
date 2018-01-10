FROM usgseros/ubuntu-gis-clj
MAINTAINER USGS LCMAP http://eros.usgs.gov

ENV version 1.0.0-RC2
ENV jarfile chipmunk-$version-standalone.jar
EXPOSE 5656

RUN mkdir /app
WORKDIR /app
COPY target/$jarfile $jarfile
COPY resources/log4j.properties log4j.properties

ENTRYPOINT java -server -jar $jarfile

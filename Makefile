CONTAINERS=`docker ps -a -q`
IMAGES=`docker images -q`
USERNAME=`whoami`
VERSION=0.1.0-SNAPSHOT

build:
	lein uberjar

docker-build:
	docker build --tag usgseros/lcmap-chipmunk:$(USERNAME) .

docker-run:
	docker run usgseros/lcmap-chipmunk:$(USERNAME)

docker-shell:
	docker run -it --entrypoint=/bin/bash usgseros/lcmap-chipmunk:$(USERNAME)

docker-compose-up:
	docker-compose -f resources/docker-compose.yml up -d

docker-compose-down:
	docker-compose -f resources/docker-compose.yml down

docker-rm-containers: docker-compose-down
	@if [ -n "$(CONTAINERS)" ]; then \
		docker rm $(CONTAINERS); fi;

docker-rm-images: docker-rm-containers
	@if [ -n "$(IMAGES)" ]; then \
		docker rmi $(IMAGES); fi;

DOCKERFILE_SERVER := Dockerfile.server
DOCKER_CONTAINER_NAME := score-server-local
DOCKER_IMAGE_NAME := overture/score-server:local

help:
	@grep '^[A-Za-z0-9_-]\+:.*' ./Makefile | sed 's/:.*//'

docker-server-clean:
	-docker kill $(DOCKER_CONTAINER_NAME)

docker-server-purge: docker-server-clean
	-docker rmi $(DOCKER_IMAGE_NAME)

docker-server-logs:
	docker logs $(DOCKER_CONTAINER_NAME)

docker-server-build: $(DOCKERFILE_SERVER)
	docker-compose build

docker-server-run: docker-server-build
	docker-compose up -d


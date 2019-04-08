VERSION := 1.6.1
DOCKERFILE_SERVER := Dockerfile.server
DOCKER_CONTAINER_NAME := score-server-$(VERSION)
DOCKER_IMAGE_NAME := overture/score-server:$(VERSION)

help:
	@grep '^[A-Za-z0-9_-]\+:.*' ./Makefile | sed 's/:.*//'

docker-server-clean:
	-docker kill $(DOCKER_CONTAINER_NAME)

docker-server-purge: docker-server-clean
	-docker rmi $(DOCKER_IMAGE_NAME)

docker-server-logs:
	docker logs $(DOCKER_CONTAINER_NAME)

docker-server-build: $(DOCKERFILE_SERVER)
	VERSION=$(VERSION) docker-compose build

docker-server-run: docker-server-build
	VERSION=$(VERSION) docker-compose up -d


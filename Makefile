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

#docker-server-build: $(DOCKERFILE_SERVER)
#	docker build --build-arg SCORE_VERSION=${VERSION} --build-arg SCORE_ARTIFACTORY_REPO_NAME=dcc-release -f $(DOCKERFILE_SERVER) -t $(DOCKER_IMAGE_NAME) ./
#
#docker-server-run: docker-server-build
#	docker run  \
#		--rm  \
#		--log-driver json-file \
#		--detach \
#		--network host \
#		--name $(DOCKER_CONTAINER_NAME) \
#		-e SERVER_PORT=8087 \
#		-e OBJECT_SENTINEL=heliograph \
#		-e BUCKET_NAME_OBJECT=oicr.icgc.test \
#		-e BUCKET_NAME_STATE=oicr.icgc.test \
#		-e COLLABORATORY_DATA_DIRECTORY=data \
#		-e METADATA_URL=https://song.cancercollaboratory.org \
#		-e S3_ENDPOINT=http://localhost:8085 \
#		-e S3_ACCESSKEY=minio \
#		-e S3_SECRETKEY=minio123 \
#		-e S3_SIGV4ENABLED=true \
#		-e AUTH_SERVER_URL=http://localhost:8084/check_token/ \
#		-e AUTH_SERVER_CLIENTID=3kJhz9pNtC0pFHAxr2SPkUkGjXrkWWqGcnPC0vBP \
#		-e AUTH_SERVER_CLIENTSECRET=v9mjRtuEVwpt7cgqnsq6mxtCa5FbUOpKLGh7WX8a1dWbBKfrM3iV3VYMtE60jr3W7GLWtNeYIaJ8EUxPkaInclWVXf64qKdR3IKwyfpDU7JhvWEwIYQYdwV1YAUZjB2e \
#		-e AUTH_SERVER_UPLOADSCOPE=collab.upload \
#		-e AUTH_SERVER_DOWNLOADSCOPE=collab.download \
#		-e SERVER_SSL_ENABLED=false \
#		-e UPLOAD_PARTSIZE=1073741824 \
#		-e UPLOAD_CONNECTION_TIMEOUT=1200000 \
#		-e LOGGING_LEVEL_BIO_OVERTURE_SCORE_SERVER=DEBUG \
#		-e LOGGING_LEVEL_ORG_APACHE_HTTP_WIRE=DEBUG \
#		-e LOGGING_LEVEL_ORG_APACHE_HTTP_HEADERS=DEBUG \
#		-e ENDPOINTS_DUMP_ENABLED=false \
#		-e ENDPOINTS_ENV_ENABLED=true \
#		-e ENDPOINTS_INFO_ENABLED=true \
#		$(DOCKER_IMAGE_NAME)


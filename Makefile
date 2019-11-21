.PHONY: 

# Override this variable to 1, for debug mode
DEMO_MODE := 0
FORCE := 0

# Required System files
DOCKER_COMPOSE_EXE := $(shell which docker-compose)
CURL_EXE := $(shell which curl)
MVN_EXE := $(shell which mvn)

# Variables
DOCKERFILE_NAME := $(shell if [ $(DEMO_MODE) -eq 1 ]; then echo Dockerfile; else echo Dockerfile.dev; fi)
ROOT_DIR := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))
THIS_USER := $$(id -u):$$(id -g)
PROJECT_NAME := $(shell echo $(ROOT_DIR) | sed 's/.*\///g')
PROJECT_VERSION := $(shell $(MVN_EXE) -f $(ROOT_DIR) help:evaluate -Dexpression=project.version -q -DforceStdout 2>&1  | tail -1)

# STDOUT Formatting
RED := $$(echo  "\033[0;31m")
YELLOW := $$(echo "\033[0;33m")
END := $$(echo  "\033[0m")
ERROR_HEADER :=  [ERROR]:
INFO_HEADER := "**************** "
DONE_MESSAGE := $(YELLOW)$(INFO_HEADER) "- done\n" $(END)

# Paths
DOCKER_DIR := $(ROOT_DIR)/docker
SCRATCH_DIR := $(DOCKER_DIR)/scratch/
SCORE_CLIENT_LOGS_DIR := $(SCRATCH_DIR)/score-client-logs
SCORE_CLIENT_LOG_FILE := $(SCORE_CLIENT_LOGS_DIR)/client.log
SCORE_SERVER_DIST_FILE := $(ROOT_DIR)/score-server/target/score-server-$(PROJECT_VERSION)-dist.tar.gz
SCORE_CLIENT_DIST_FILE := $(ROOT_DIR)/score-client/target/score-client-$(PROJECT_VERSION)-dist.tar.gz
RETRY_CMD := $(DOCKER_DIR)/retry-command.sh

# Commands
DOCKER_COMPOSE_CMD := echo "*********** DEMO_MODE = $(DEMO_MODE) **************" \
	&& echo "*********** FORCE = $(FORCE) **************" \
	&& DOCKERFILE_NAME=$(DOCKERFILE_NAME) $(DOCKER_COMPOSE_EXE) -f $(ROOT_DIR)/docker-compose.yml
SCORE_CLIENT_CMD := $(DOCKER_COMPOSE_CMD) run --rm -u $(THIS_USER) score-client bin/score-client
SCORE_CLIENT_TEST := $(DOCKER_COMPOSE_CMD) run --rm -u $(THIS_USER) score-client /data/run_tests.sh
SCORE_CLIENT_MANIFEST_TEST := $(DOCKER_COMPOSE_CMD) run --rm -u $(THIS_USER) score-client /data/test_manifests.sh
DC_UP_CMD := $(DOCKER_COMPOSE_CMD) up -d --build
MVN_CMD := $(MVN_EXE) -f $(ROOT_DIR)/pom.xml

#############################################################
# Internal Targets
#############################################################
$(SCORE_CLIENT_LOG_FILE):
	@mkdir -p $(SCORE_CLIENT_LOGS_DIR)
	@touch $(SCORE_CLIENT_LOGS_DIR)/client.log
	@chmod 777 $(SCORE_CLIENT_LOGS_DIR)/client.log

_build-score-client: package
	@$(DOCKER_COMPOSE_CMD) build score-client

_ping_score_server:
	@echo $(YELLOW)$(INFO_HEADER) "Pinging score-server on http://localhost:8087" $(END)
	@$(RETRY_CMD) curl  \
		-XGET \
		-H 'Authorization: Bearer f69b726d-d40f-4261-b105-1ec7e6bf04d5' \
		'http://localhost:8087/download/ping'
	@echo ""

_ping_song_server:
	@echo $(YELLOW)$(INFO_HEADER) "Pinging song-server on http://localhost:8080" $(END)
	@$(RETRY_CMD) curl --connect-timeout 5 \
		--max-time 10 \
		--retry 5 \
		--retry-delay 0 \
		--retry-max-time 40 \
		--retry-connrefuse \
		'http://localhost:8080/isAlive'
	@echo ""


_setup-object-storage: 
	@echo $(YELLOW)$(INFO_HEADER) "Setting up bucket oicr.icgc.test and heliograph" $(END)
	@if  $(DOCKER_COMPOSE_CMD) run aws-cli --endpoint-url http://object-storage:9000 s3 ls s3://oicr.icgc.test ; then \
		echo $(YELLOW)$(INFO_HEADER) "Bucket already exists. Skipping creation..." $(END); \
	else \
		$(DOCKER_COMPOSE_CMD) run aws-cli --endpoint-url http://object-storage:9000 s3 mb s3://oicr.icgc.test; \
	fi
	@$(DOCKER_COMPOSE_CMD) run aws-cli --endpoint-url http://object-storage:9000 s3 cp /score-data/heliograph s3://oicr.icgc.test/data/heliograph

_destroy-object-storage:
	@echo $(YELLOW)$(INFO_HEADER) "Removing bucket oicr.icgc.test" $(END)
	@if  $(DOCKER_COMPOSE_CMD) run aws-cli --endpoint-url http://object-storage:9000 s3 ls s3://oicr.icgc.test ; then \
		$(DOCKER_COMPOSE_CMD) run aws-cli --endpoint-url http://object-storage:9000 s3 rb s3://oicr.icgc.test --force; \
	else \
		echo $(YELLOW)$(INFO_HEADER) "Bucket does not exist. Skipping..." $(END); \
	fi

_setup: $(SCORE_CLIENT_LOG_FILE)

#############################################################
# Help
#############################################################

# Help menu, displaying all available targets
help:
	@echo
	@echo "**************************************************************"
	@echo "**************************************************************"
	@echo "To dry-execute a target run: make -n <target> "
	@echo
	@echo "Available Targets: "
	@grep '^[A-Za-z][A-Za-z0-9_-]\+:.*' $(ROOT_DIR)/Makefile | sed 's/:.*//' | sed 's/^/\t/'
	@echo

# Outputs the Docker run profile in IntelliJ to debug score-client
intellij-score-client-config: _build-score-client
	@echo
	@echo
	@echo $(YELLOW)$(INFO_HEADER) In IntelliJ, configure the docker run profile with the following parameters to allow interactive debug on port 5005 $(END)
	@echo "$(YELLOW)Image ID:$(END)               $(PROJECT_NAME)_score-client:latest"
	@echo "$(YELLOW)Command:$(END)                bin/score-client upload --manifest /data/combined.manifest"
	@echo "$(YELLOW)Bind Mounts:$(END)            $(DOCKER_DIR)/score-client-init:/data $(SCRATCH_DIR)/score-client/logs:/score-client/logs"
	@echo "$(YELLOW)Environment Variables:$(END)  ACCESSTOKEN=f69b726d-d40f-4261-b105-1ec7e6bf04d5; METADATA_URL=http://song-server:8080; STORAGE_URL=http://score-server:8080; JAVA_TOOL_OPTIONS=-agentlib:jdwp=transport=dt_socket,address=*:5005,server=y,suspend=n"
	@echo "$(YELLOW)Run Options:$(END)            --rm --network $(PROJECT_NAME)_default"
	@echo
	@echo
	@echo $(YELLOW)$(INFO_HEADER) After configuring docker run profile, configure debug profile with port forwarding 5005:5005 $(END)

#############################################################
#  Cleaning targets
#############################################################

# Brings down all docker-compose services
nuke:
	@echo $(YELLOW)$(INFO_HEADER) "Destroying running docker services" $(END)
	@$(DOCKER_COMPOSE_CMD) down -v

# Kills running services and removes created files/directories
clean-docker: nuke
	@echo $(YELLOW)$(INFO_HEADER) "Deleting generated files" $(END)

# Maven clean
clean-mvn:
	@echo $(YELLOW)$(INFO_HEADER) "Cleaning maven" $(END)
	@$(MVN_CMD) clean

# Just kill and delete the score-server container
clean-score-server:
	@echo $(YELLOW)$(INFO_HEADER) "Killing and Cleaning score-server" $(END)
	@$(DOCKER_COMPOSE_CMD) kill score-server
	@$(DOCKER_COMPOSE_CMD) rm -f score-server

# Delete all objects from object storage
clean-objects: _destroy-object-storage

# Clean everything. Kills all services, maven cleans and removes generated files/directories
clean: clean-docker clean-mvn

reset-object-storage: clean-objects _setup-object-storage

#############################################################
#  Building targets
#############################################################
_test: 
	@$(MVN_CMD) test

# Package the score-server and score-client using maven. Affected by DEMO_MODE and FORCE
package: 
	@if [ $(DEMO_MODE) -eq 0 ] && ( [ ! -e $(SCORE_SERVER_DIST_FILE) ] ||  [ ! -e $(SCORE_CLIENT_DIST_FILE) ]) ; then \
		echo $(YELLOW)$(INFO_HEADER) "Running maven package" $(END); \
		$(MVN_CMD) package -DskipTests; \
	elif [ $(DEMO_MODE) -ne 0 ]; then \
		echo $(YELLOW)$(INFO_HEADER) "Skipping maven package since DEMO_MODE=$(DEMO_MODE)" $(END); \
	elif [ $(FORCE) -eq 1 ]; then \
		echo $(YELLOW)$(INFO_HEADER) "Forcefully runnint maven package since FORCE=$(FORCE)" $(END); \
		$(MVN_CMD) package -DskipTests; \
	else \
		echo $(YELLOW)$(INFO_HEADER) "Skipping maven package since files exist: $(SCORE_SERVER_DIST_FILE)   $(SCORE_CLIENT_DIST_FILE)" $(END); \
	fi



#############################################################
#  Docker targets
#############################################################

rebuild-client: clean-mvn package 
	@echo $(YELLOW)$(INFO_HEADER) "Rebuilding client docker" $(END)
	@$(DOCKER_COMPOSE_CMD) build score-client

rebuild-server: clean-mvn package 
	@echo $(YELLOW)$(INFO_HEADER) "Rebuilding server docker" $(END)
	@$(DOCKER_COMPOSE_CMD) build score-server

rebuild-all: clean-mvn package
	@$(DOCKER_COMPOSE_CMD) build score-server score-client

# Start ego, song, and object-storage.
start-deps: _setup package
	@echo $(YELLOW)$(INFO_HEADER) "Starting dependencies: ego, song and object-storage" $(END)
	@$(DC_UP_CMD) ego-api song-server object-storage

# Start score-server and all dependencies. Affected by DEMO_MODE
start-score-server: _setup package start-deps _setup-object-storage
	@echo $(YELLOW)$(INFO_HEADER) "Starting score-server" $(END)
	@$(DC_UP_CMD) score-server

# Display logs for score-server
log-score-server:
	@echo $(YELLOW)$(INFO_HEADER) "Displaying logs for score-server" $(END)
	@$(DOCKER_COMPOSE_CMD) logs score-server

# Display logs for score-client
log-score-client:
	@echo $(YELLOW)$(INFO_HEADER) "Displaying logs for score-server" $(END)
	@$(DOCKER_COMPOSE_CMD) logs score-server


#############################################################
#  Client targets
#############################################################
get-client-command:
	@echo "$(SCORE_CLIENT_CMD)"

# Upload & download object-id with different access tokens. 
# Affected by DEMO_MODE
test-manifest: start-score-server _ping_score_server _ping_song_server
	@echo $(YELLOW)$(INFO_HEADER) "Testing uploads/download manifests" $(END)
	@$(MAKE) reset-object-storage
	@$(SCORE_CLIENT_MANIFEST_TEST)

test-upload-and-download: start-score-server _ping_score_server _ping_song_server
	@echo $(YELLOW)$(INFO_HEADER) "Testing upload & download permissions" $(END)
	@$(MAKE) reset-object-storage
	@$(SCORE_CLIENT_TEST)

test: _test test-manifest test-upload-and-download

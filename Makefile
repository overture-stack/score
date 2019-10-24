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
ACCESS_TOKEN := f69b726d-d40f-4261-b105-1ec7e6bf04d5
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
DC_UP_CMD := $(DOCKER_COMPOSE_CMD) up -d --build
MVN_CMD := $(MVN_EXE) -f $(ROOT_DIR)/pom.xml

#############################################################
# Internal Targets
#############################################################
$(SCORE_CLIENT_LOG_FILE):
	@mkdir -p $(SCORE_CLIENT_LOGS_DIR)
	@touch $(SCORE_CLIENT_LOGS_DIR)/client.log
	@chmod 777 $(SCORE_CLIENT_LOGS_DIR)/client.log

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
	@echo "                  Help"
	@echo "**************************************************************"
	@echo "To dry-execute a target run: make -n <target> "
	@echo
	@echo "Available Targets: "
	@grep '^[A-Za-z][A-Za-z0-9_-]\+:.*' $(ROOT_DIR)/Makefile | sed 's/:.*//' | sed 's/^/\t/'
	@echo

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
	@sudo rm -rf $(DOCKER_DIR)/scratch

# Maven clean
clean-mvn:
	@echo $(YELLOW)$(INFO_HEADER) "Cleaning maven" $(END)
	@$(MVN_CMD) clean

# Just kill and delete the score-server container
clean-score-server:
	@echo $(YELLOW)$(INFO_HEADER) "Killing and Cleaning score-server" $(END)
	@$(DOCKER_COMPOSE_CMD) kill score-server
	@$(DOCKER_COMPOSE_CMD) rm score-server

# Delete all objects from object storage
clean-objects: _destroy-object-storage

# Clean everything. Kills all services, maven cleans and removes generated files/directories
clean: clean-docker clean-mvn

#############################################################
#  Building targets
#############################################################

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
#  Song targets
#############################################################

# Publishes the analysis. Used before running the test-download target
song-publish:
	@echo $(YELLOW)$(INFO_HEADER) "Publishing analysis" $(END)
	@$(CURL_EXE) -XPUT --header 'Authorization: Bearer $(ACCESS_TOKEN)' 'http://localhost:8080/studies/ABC123/analysis/publish/735b65fa-f502-11e9-9811-6d6ef1d32823'
	@echo ""

# UnPublishes the analysis. Used before running the test-download target
song-unpublish:
	@echo $(YELLOW)$(INFO_HEADER) "UnPublishing analysis" $(END)
	@$(CURL_EXE) -XPUT --header 'Authorization: Bearer $(ACCESS_TOKEN)' 'http://localhost:8080/studies/ABC123/analysis/unpublish/735b65fa-f502-11e9-9811-6d6ef1d32823'
	@echo ""

#############################################################
#  Client targets
#############################################################

# Upload a manifest using the score-client. Affected by DEMO_MODE
test-upload: start-score-server _ping_score_server
	@echo $(YELLOW)$(INFO_HEADER) "Uploading test /data/manifest.txt" $(END)
	@$(SCORE_CLIENT_CMD) upload --manifest /data/manifest.txt

# Download an object-id. Affected by DEMO_MODE
test-download: start-score-server _ping_score_server _ping_song_server song-publish
	@echo $(YELLOW)$(INFO_HEADER) "Downlaoding test object id" $(END)
	@$(SCORE_CLIENT_CMD) download --object-id 5be58fbb-775b-5259-bbbd-555e07fbdf24 --output-dir /tmp



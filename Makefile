.PHONY:

# Override this variable to 1, for debug mode
DEMO_MODE := 0
FORCE := 0

# Variables
DOCKERFILE_NAME := $(shell if [ $(DEMO_MODE) -eq 1 ]; then echo Dockerfile; else echo Dockerfile.dev; fi)
ROOT_DIR := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))
THIS_USER := $$(id -u):$$(id -g)
ACCESS_TOKEN := f69b726d-d40f-4261-b105-1ec7e6bf04d5
PROJECT_VERSION := $$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout 2>&1  | tail -1)

# Required System files
DOCKER_COMPOSE_EXE := $(shell which docker-compose)
CURL_EXE := $(shell which curl)

# STDOUT Formatting
RED := $$(echo  "\033[0;31m")
YELLOW := $$(echo "\033[0;33m")
END := $$(echo  "\033[0m")
ERROR_HEADER :=  [ERROR]:
INFO_HEADER := "**************** "
DONE_MESSAGE := $(YELLOW)$(INFO_HEADER) "- done\n" $(END)

# Paths
DOCKER_DIR := ./docker
SCRATCH_DIR := $(DOCKER_DIR)/scratch/
SCORE_CLIENT_LOGS_DIR := $(SCRATCH_DIR)/score-client-logs
SCORE_CLIENT_LOG_FILE := $(SCORE_CLIENT_LOGS_DIR)/client.log
SCORE_SERVER_DIST_FILE := ./score-server/target/score-server-$(PROJECT_VERSION)-dist.tar.gz
SCORE_CLIENT_DIST_FILE := ./score-client/target/score-client-$(PROJECT_VERSION)-dist.tar.gz

# Commands
DOCKER_COMPOSE_CMD := echo "*********** DEMO_MODE = $(DEMO_MODE) **************" \
	&& echo "*********** FORCE = $(FORCE) **************" \
	&& DOCKERFILE_NAME=$(DOCKERFILE_NAME) $(DOCKER_COMPOSE_EXE)
SCORE_CLIENT_CMD := $(DOCKER_COMPOSE_CMD) run --rm -u $(THIS_USER) score-client bin/score-client
DC_UP_CMD := $(DOCKER_COMPOSE_CMD) up -d --build

# Internal Targets
$(SCORE_CLIENT_LOG_FILE):
	@mkdir -p $(SCORE_CLIENT_LOGS_DIR)
	@touch $(SCORE_CLIENT_LOGS_DIR)/client.log
	@chmod 777 $(SCORE_CLIENT_LOGS_DIR)/client.log

_setup: $(SCORE_CLIENT_LOG_FILE)

# Help menu, displaying all available targets
help:
	@grep '^[A-Za-z0-9_-]\+:.*' ./Makefile | sed 's/:.*//'


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
	@sudo rm -rf $(DOCKER_DIR)/object-storage-init/data/.minio.sys
	@find $(DOCKER_DIR)/object-storage-init/data/ -type f | grep -v heliograph | xargs sudo rm -rf
	@sudo rm -rf $(DOCKER_DIR)/scratch

# Maven clean
clean-mvn:
	@echo $(YELLOW)$(INFO_HEADER) "Cleaning maven" $(END)
	@mvn clean

# Just kill and delete the score-server container
clean-score-server:
	@echo $(YELLOW)$(INFO_HEADER) "Killing and Cleaning score-server" $(END)
	@$(DOCKER_COMPOSE_CMD) kill score-server
	@$(DOCKER_COMPOSE_CMD) rm score-server

# Delete all objects from object storage
clean-objects:
	@echo $(YELLOW)$(INFO_HEADER) "Deleting all objects from object-storage" $(END)
	@find $(DOCKER_DIR)/object-storage-init/data/ -type f | grep -v heliograph | xargs sudo rm -rf

# Clean everything. Kills all services, maven cleans and removes generated files/directories
clean: clean-docker clean-mvn

#############################################################
#  Building targets
#############################################################

# Package the score-server and score-client using maven. Affected by DEMO_MODE and FORCE
package: 
	@if [ $(DEMO_MODE) -eq 0 ] && ( [ ! -e $(SCORE_SERVER_DIST_FILE) ] ||  [ ! -e $(SCORE_CLIENT_DIST_FILE) ]) ; then \
		echo $(YELLOW)$(INFO_HEADER) "Running maven package" $(END); \
		mvn package -DskipTests; \
	elif [ $(DEMO_MODE) -ne 0 ]; then \
		echo $(YELLOW)$(INFO_HEADER) "Skipping maven package since DEMO_MODE=$(DEMO_MODE)" $(END); \
	elif [ $(FORCE) -eq 1 ]; then \
		echo $(YELLOW)$(INFO_HEADER) "Forcefully runnint maven package since FORCE=$(FORCE)" $(END); \
		mvn package -DskipTests; \
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
start-score-server: _setup package
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

#############################################################
#  Client targets
#############################################################

# Upload a manifest using the score-client. Affected by DEMO_MODE
test-upload: start-score-server
	@echo $(YELLOW)$(INFO_HEADER) "Uploading test /data/manifest.txt" $(END)
	@$(SCORE_CLIENT_CMD) upload --manifest /data/manifest.txt

# Download an object-id. Affected by DEMO_MODE
test-download: start-score-server song-publish
	@echo $(YELLOW)$(INFO_HEADER) "Downlaoding test object id" $(END)
	@$(SCORE_CLIENT_CMD) download --object-id 5be58fbb-775b-5259-bbbd-555e07fbdf24 --output-dir /tmp



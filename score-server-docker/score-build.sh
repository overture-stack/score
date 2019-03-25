#!/bin/bash
VERSION=1.6.1
docker build --build-arg SCORE_VERSION=${VERSION} --build-arg SCORE_ARTIFACTORY_REPO_NAME=dcc-release -f Dockerfile.score -t overture/score-server:${VERSION} ./

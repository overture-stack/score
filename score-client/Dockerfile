#
#    _____ __________  ____          _________            __
#   / ___// ____/ __ \/ __ \___     / ____/ (_)__  ____  / /_
#   \__ \/ /   / / / / /_/ / _ \   / /   / / / _ \/ __ \/ __/
#  ___/ / /___/ /_/ / _, _/  __/  / /___/ / /  __/ / / / /_
# /____/\____/\____/_/ |_|\___/   \____/_/_/\___/_/ /_/\__/
#
# Banner @ https://goo.gl/Yyoc6R

FROM       ubuntu:16.04
MAINTAINER ICGC <dcc-support@icgc.org>

#
# Update apt, add FUSE support and basic command line tools
#

RUN \
  apt-get update && \
  apt-get -y upgrade && \
  apt-get install -y libfuse-dev fuse curl wget software-properties-common

#
# Install Oracle JDK 8
#
RUN apt install -y openjdk-8-jdk

ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64

#
# Install latest version of score client distribution
#

RUN mkdir -p /score-client && \
    cd /score-client && \
    wget -qO- https://artifacts.oicr.on.ca/artifactory/dcc-release/bio/overture/score-client/[RELEASE]/score-client-[RELEASE]-dist.tar.gz | \
    tar xvz --strip-components 1

#
# Set working directory for convenience with interactive usage
#

WORKDIR /score-client

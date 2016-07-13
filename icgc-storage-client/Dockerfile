#     ______________________   _____ __                                 _________            __ 
#    /  _/ ____/ ____/ ____/  / ___// /_____  _________ _____ ____     / ____/ (_)__  ____  / /_
#    / // /   / / __/ /       \__ \/ __/ __ \/ ___/ __ `/ __ `/ _ \   / /   / / / _ \/ __ \/ __/
#  _/ // /___/ /_/ / /___    ___/ / /_/ /_/ / /  / /_/ / /_/ /  __/  / /___/ / /  __/ / / / /_  
# /___/\____/\____/\____/   /____/\__/\____/_/   \__,_/\__, /\___/   \____/_/_/\___/_/ /_/\__/  
#                                                    /____/                                    
# Banner @ http://goo.gl/VCY0tD

FROM       ubuntu:14.04
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

RUN add-apt-repository ppa:webupd8team/java
RUN apt-get update && apt-get upgrade -y
RUN dpkg -r --force-all oracle-java8-installer
RUN echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections
RUN apt-get install -y \
    oracle-java8-installer \
    oracle-java8-set-default && \
    rm -rf /var/lib/apt/lists/* && \
    rm -rf /var/cache/oracle-jdk8-installer

# Define commonly used JAVA_HOME variable
ENV JAVA_HOME /usr/lib/jvm/java-8-oracle

# 
# Install latest version of storage client distribution
#

RUN mkdir -p /icgc/icgc-storage-client && \
    cd /icgc/icgc-storage-client && \
    wget -qO- https://artifacts.oicr.on.ca/artifactory/dcc-release/org/icgc/dcc/icgc-storage-client/[RELEASE]/icgc-storage-client-[RELEASE]-dist.tar.gz | \
    tar xvz --strip-components 1

#
# Set working directory for convenience with interactive usage
#

WORKDIR /icgc/icgc-storage-client

FROM ubuntu:latest
MAINTAINER Benjamin Ran <benjaminran@ucsc.edu>

# install oracle java
RUN apt-get update \
 && echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu xenial main" | tee /etc/apt/sources.list.d/webupd8team-java.list \
 && echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu xenial main" | tee -a /etc/apt/sources.list.d/webupd8team-java.list \
 && apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886 \
 && apt-get update \
 && echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections \
 && apt-get -y install oracle-java8-installer

# add dcc files
ENV DCC_HOME /dcc
RUN mkdir -p ${DCC_HOME}
COPY ./bin ${DCC_HOME}/bin
COPY ./cert ${DCC_HOME}/cert
COPY ./conf ${DCC_HOME}/conf
COPY ./lib ${DCC_HOME}/lib
COPY ./logs ${DCC_HOME}/logs

# development truststore password
ENV REDWOOD_TRUSTSTORE_PASSWORD password

ENV PATH $PATH:${DCC_HOME}/bin
RUN dcc-storage-server install

EXPOSE 5431

WORKDIR ${DCC_HOME}

CMD dcc-storage-server start && while [ `ls ${DCC_HOME}/logs | wc -w` -lt 3 ]; do sleep 1; done && tail -f ${DCC_HOME}/logs/*.log

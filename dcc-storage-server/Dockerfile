FROM ubuntu:latest

# install oracle java
RUN apt-get update
RUN apt-get install -y software-properties-common
RUN add-apt-repository -y ppa:webupd8team/java
RUN apt-get update
RUN echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections
RUN apt-get install -y oracle-java8-installer

# add dcc files
ENV DCC_HOME /dcc
RUN mkdir -p $DCC_HOME && mkdir $DCC_HOME/conf && mkdir $DCC_HOME/dcc-storage-server

ARG TRUST_STORE
ARG KEY_STORE
COPY ${TRUST_STORE} ${KEY_STORE} $DCC_HOME/conf/ssl/

ARG DCC_ARCHIVE
COPY ${DCC_ARCHIVE:-./target/dcc-storage-server-1.0.22-SNAPSHOT-dist.tar.gz} $DCC_HOME/dcc-storage-server.tar.gz
WORKDIR $DCC_HOME
RUN tar xvf $DCC_HOME/dcc-storage-server.tar.gz -C $DCC_HOME/dcc-storage-server --strip-components=1
ENV PATH $PATH:$DCC_HOME/dcc-storage-server/bin
RUN dcc-storage-server install

EXPOSE 5431

CMD dcc-storage-server start && while [ `ls $DCC_HOME/dcc-storage-server/logs | wc -w` -lt 3 ]; do sleep 1; done && tail -f $DCC_HOME/dcc-storage-server/logs/*.log

FROM java:8-jre
MAINTAINER OVERTURE <contact@overture.bio>

ENV DCC_HOME /opt/dcc
ENV DCC_DATA $DCC_HOME/data
ENV DCC_CONFIG $DCC_HOME/config
ENV DCC_SCRIPTS $DCC_HOME/scripts
ENV SCORE_SERVER_VERSION 1.6.1
ENV SCORE_HOME $DCC_HOME/score-server
ENV SCORE_LOGS $SCORE_HOME/logs
ENV DOWNLOAD_URL https://artifacts.oicr.on.ca/artifactory/dcc-release/bio/overture/score-server/${SCORE_SERVER_VERSION}/score-server-${SCORE_SERVER_VERSION}-dist.tar.gz
ENV TARBALL $DCC_HOME/score-server.tar.gz
ENV EXPAND_SCRIPT $DCC_HOME/tools/expand.py
ENV INPUT_FILE $DCC_CONFIG/application.properties.template
ENV OUTPUT_FILE $SCORE_HOME/conf/application-secure.properties

RUN apt update &&  \
    apt install -y wget git sqlite3 python3 python3-pip build-essential libssl-dev python3-dev nmap vim curl telnet postgresql && \
    useradd -m dcc && \
    echo "alias ls='ls --color'" >> ~/.bashrc && \
    echo "alias ll='ls -l'" >> ~/.bashrc && \
    echo "alias lr='ll -rt'" >> ~/.bashrc && \
    echo "alias la='ll -a'" >> ~/.bashrc && \
    mkdir -p $DCC_HOME/storage_server_logs
WORKDIR $DCC_HOME

COPY config $DCC_CONFIG
COPY tools ${DCC_HOME}/tools
COPY data ${DCC_HOME}/data

RUN wget $DOWNLOAD_URL -O $TARBALL && \
            tar zxvf $TARBALL && \
            mv -f $DCC_HOME/score-server-*  $SCORE_HOME && \
            rm -rf $TARBALL $SCORE_HOME/conf/application.* && \
            mv $SCORE_LOGS  ${SCORE_LOGS}.bak  && \
            ln -s $DCC_HOME/storage_server_logs $SCORE_LOGS

CMD python3 $EXPAND_SCRIPT $INPUT_FILE $OUTPUT_FILE && \
        $SCORE_HOME/bin/score-server start  \
        wrapper.java.additional.2=-Dcom.sun.management.jmxremote.port=${STORAGE_SERVER_JMX_PORT} \
        wrapper.app.parameter.6=--server.port=${STORAGE_SERVER_PORT} \
        wrapper.app.parameter.5=--spring.profiles.active=amazon,collaboratory,prod,secure \
        ;FOR_100_YEARS=$((100*365*24*60*60));while true;do sleep $FOR_100_YEARS;done

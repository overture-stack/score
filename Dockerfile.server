FROM openjdk:8-jdk-alpine as builder 

RUN apk update && apk upgrade && \
    apk add --no-cache maven

ENV JAR_FILE            /score-server.jar

# Build song-server jar
WORKDIR /srv
COPY . /srv
RUN mvn package -DskipTests \
    && cd score-server/target \
    && mv score-server-*-dist.tar.gz score-server.tar.gz \
    && tar zxvf score-server.tar.gz -C /tmp \
    && mv -f /tmp/score-server-*  /tmp/score-dist  \
    && cp -f /tmp/score-dist/lib/score-server.jar $JAR_FILE

###############################################################################################################

FROM openjdk:8-jre-alpine

# Paths
ENV SCORE_HOME /score-server
ENV SCORE_LOGS $SCORE_HOME/logs
ENV JAR_FILE            /score-server.jar

COPY --from=builder $JAR_FILE $JAR_FILE

WORKDIR $SCORE_HOME

CMD mkdir -p  $SCORE_HOME $SCORE_LOGS \
    && java -Dlog.path=$SCORE_LOGS \
    -jar $JAR_FILE \
    --spring.config.location=classpath:/application.yml \
    --spring.profiles.active=amazon,collaboratory,prod,secure

#&& FOR_100_YEARS=$((100*365*24*60*60));while true;do sleep $FOR_100_YEARS;done

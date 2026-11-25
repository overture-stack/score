###############################
# Maven source
###############################
FROM adoptopenjdk/openjdk11:jdk-11.0.6_10-slim AS source

WORKDIR /srv
COPY . /srv

###############################
# Score Client builder
###############################
FROM source AS client-builder

ENV CLIENT_DIST_DIR=/score-client-dist

# Build score-client and the reactor modules it depends on.
RUN ./mvnw -pl score-client -am package -DskipTests

# Prepare client dist
RUN cd score-client/target \
    && mv score-client-*-dist.tar.gz score-client.tar.gz \
    && tar zxvf score-client.tar.gz -C /tmp \
    && mv -f /tmp/score-client-* /tmp/score-client-dist \
    && cp -r /tmp/score-client-dist $CLIENT_DIST_DIR \
    && mkdir -p $CLIENT_DIST_DIR/logs \
    && touch $CLIENT_DIST_DIR/logs/client.log \
    && chmod 777 $CLIENT_DIST_DIR/logs/client.log

###############################
# Score Client
###############################
FROM ubuntu:24.04 AS client

ENV CLIENT_DIST_DIR=/score-client-dist
ENV SCORE_CLIENT_HOME=/score-client
ENV SCORE_USER=score
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:$SCORE_CLIENT_HOME/bin:$JAVA_HOME/bin

# Add score user, update apt, add FUSE support and basic command line tools
RUN useradd $SCORE_USER \
    && apt-get update \
    && apt-get install -y --no-install-recommends \
    ca-certificates \
    curl \
    fuse3 \
    libfuse2 \
    openjdk-17-jre-headless \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir $SCORE_CLIENT_HOME

# Copy client dist from the client builder stage
COPY --from=client-builder $CLIENT_DIST_DIR/ $SCORE_CLIENT_HOME

RUN java -version \
    && chown -R $SCORE_USER:$SCORE_USER $SCORE_CLIENT_HOME

# Set working directory for convenience with interactive usage
WORKDIR $SCORE_CLIENT_HOME

###############################
# Score Server
###############################
FROM source AS server-builder

ENV SERVER_JAR_FILE=/score-server.jar

RUN ./mvnw -pl score-server -am package -DskipTests

RUN cd score-server/target \
    && mv score-server-*-dist.tar.gz score-server.tar.gz \
    && tar zxvf score-server.tar.gz -C /tmp \
    && mv -f /tmp/score-server-* /tmp/score-server-dist \
    && cp -f /tmp/score-server-dist/lib/score-server.jar $SERVER_JAR_FILE

FROM adoptopenjdk/openjdk11:jre-11.0.6_10 AS server

# Paths
ENV SCORE_HOME /score-server
ENV SCORE_LOGS $SCORE_HOME/logs
ENV JAR_FILE            /score-server.jar
ENV SCORE_USER score
ENV SCORE_UID 9999
ENV SCORE_GID 9999

RUN addgroup --system --gid $SCORE_GID $SCORE_USER  \
    && adduser --system --uid $SCORE_UID --ingroup $SCORE_USER $SCORE_USER  \
    && mkdir $SCORE_HOME $SCORE_LOGS \
    && chown -R $SCORE_UID:$SCORE_GID $SCORE_HOME

COPY --from=server-builder $JAR_FILE $JAR_FILE

USER $SCORE_UID

WORKDIR $SCORE_HOME

CMD java -Dlog.path=$SCORE_LOGS \
    -jar $JAR_FILE \
    --spring.config.location=classpath:/application.yml,classpath:/bootstrap.properties
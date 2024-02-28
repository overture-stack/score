# Operational Docs

Welcome to the Operational Documentation for setting up and managing the Score Server in a development environment. This document provides detailed steps and configurations needed to get your development environment up and running with either a standalone setup or using Docker. This page also includes instructions for integrating Keycloak for authentication and authorization.

## Table of Contents

- [Setting up the Development Environment](#setting-up-the-development-environment)
  - [Standalone score-server](#standalone-score-server)
    - [Clone the Score Repository](#clone-the-score-repository)
    - [Build](#build)
    - [Start the Server](#start-the-server)
  - [Docker for Score](#docker-for-score)
    - [Start score-server and all dependencies](#start-score-server-and-all-dependencies)
    - [Start the score-server (Apple Silicon Users)](#start-the-score-server-mac-m1-users)
    - [Stop score-server and clean up](#stop-score-server-and-clean-up)
- [Integrating Keycloak](#integrating-keycloak)
  - [Standalone](#standalone)
  - [Docker](#docker)

## Setting up the Development Environment

### Standalone score-server

#### Clone the Score Repository   

Clone the Score repository to your local computer: [https://github.com/overture-stack/score.git](https://github.com/overture-stack/score.git)

#### Build

[JDK11](https://www.oracle.com/ca-en/java/technologies/downloads/) and [Maven3](https://maven.apache.org/download.cgi) are required to set up this service. To build the score-server run the following command from the score directory:

```bash
./mvnw clean install -DskipTests
```

#### Start the server

Before running your score-server, ensure that your local machine is connected and running the following dependent services:

- Song Server
- Object Storage Service (S3 compatible storage or Azure)
- Authentication/Authorization Service (EGO or Keycloak)

Set the configuration of above dependent services on `score-server/src/main/resources/application.yml` and make sure to use the profiles according to your needs.

**Profiles**
| Profile | Description |
| - | - |
| *default* | Required to load default controllers |
| *secure* | Required to load security configuration |
| *collaboratory* or *azure* | Required to choose between S3 compatible or Azure storage |
| *dev* | (Optional) to facilitate dev default configuration |

Run the following command to start the score-server:

```bash
cd score-server/
mvn spring-boot:run -Dspring-boot.run.profiles=default,collaboratory,secure,dev
```

>**Warning:** This guide is meant to demonstrate the configuration and usage of Score for development purposes and is not intended for production. If you ignore this warning and use this in any public or production environment, please remember to use Spring profiles accordingly. For production do not use ***dev*** profile.

### Docker for Score

Several `make` targets are provided for locally deploying the dependent services using Docker. As the developer, you can replicate a live environment for `score-server` and `score-client`. Using Docker allows you to develop locally, test submissions, create manifests, publish, unpublish and test score uploads/downloads in an isolated environment.

For more information on the different targets, run `make help` or read the comments above each target for a description.

> Note: We will need an internet connection for the `make` command, which may take several minutes to build. No external services are required for the `make` command.

#### Start score-server and all dependencies.

To start score-server and all dependencies, use the following command:

```bash
make clean start-score-server
```

#### Start the score-server (Apple Silicon Users)

If using Apple Silicon you must set the Docker BuildKit environment variable to the legacy builder.

```bash
DOCKER_BUILDKIT=0 make clean start-score-server
```

#### Stop score-server and clean up

To clean everything, including killing all services, maven cleaning, and removing generated files/directories, use the following command:

```bash
make clean
```

> **Warning** Docker for Score is meant to demonstrate the configuration and usage of Score and is not intended for production. If you ignore this warning and use this in any public or production environment, please remember to change the passwords, accessKeys, and secretKeys.

## Integrating Keycloak

[Keycloak](https://www.keycloak.org/) is an open-source identity and access management solution that can be used to manage users and application permissions. You can find basic information on integrating Score and Keycloak using docker from our user docs [located here](https://www.overture.bio/documentation/score/docker-install/configuration/authentication/). For a comprehensive guide on installing and configuring Keycloak, refer to the [Keycloak documentation](https://www.keycloak.org/documentation).

### Standalone:

If you’re building score using the source code, the following configuration is required in `score-server/src/main/resources/application.yml`.

```yaml
auth:
  server:
    url: http://localhost/realms/myrealm/apikey/check_api_key/
    tokenName: apiKey
    clientID: score
    clientSecret: scoresecret
    provider: keycloak
    keycloak:
      host: http://localhost
      realm: "myrealm"

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://localhost/realms/myrealm/protocol/openid-connect/certs
```

### Docker

Run the following compose to spin up dependant services:

```yaml
version: '2.3'
services:
  postgresql:
    image: docker.io/bitnami/postgresql:16
    environment:
      # ALLOW_EMPTY_PASSWORD is recommended only for development.
      - ALLOW_EMPTY_PASSWORD=yes
      - POSTGRESQL_USERNAME=bn_keycloak
      - POSTGRESQL_DATABASE=bitnami_keycloak
    volumes:
      - 'postgresql_data:/bitnami/postgresql'

  keycloak:
    build: .
    depends_on:
      - postgresql
    ports:
      - "80:8080"
      # remote debugging port is recommended only for development
      # - "8787:8787"
    environment:
      # remote debugging is recommended only for development
      # - DEBUG=true
      # - DEBUG_PORT=*:8787
      - KC_DB=postgres
      - KC_DB_URL=jdbc:postgresql://postgresql/bitnami_keycloak
      - KC_DB_USERNAME=bn_keycloak
    volumes:
      - type: bind
        source: ./target/dasniko.keycloak-2fa-sms-authenticator.jar
        target: /opt/bitnami/keycloak/providers/keycloak-sms-auth.jar
      - type: bind
        source: data_import
        target: /opt/bitnami/keycloak/data/import
    volumes:
    postgresql_data:
        driver: local
  minio:
    image: minio/minio:RELEASE.2018-05-11T00-29-24Z
    ports:
      - 9000:9000
    environment:
      MINIO_ACCESS_KEY: minio
      MINIO_SECRET_KEY: minio123
    command: server /data
```

Create a `.env.score` file with the necessary environment variables:

```bash
# ============================
# Spring Run Profiles (Required)
# ============================

# Active profiles to determine app behavior & configs
SPRING_PROFILES_ACTIVE=collaboratory,prod,secure

# Server configuration
SERVER_PORT=8087
SERVER_SSL_ENABLED=false

# Logging
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB=INFO
LOGGING_LEVEL_BIO_OVERTURE_SCORE_SERVER=INFO
LOGGING_LEVEL_ROOT=INFO

# ============================
# Server Authentication integration (Required)
# ============================
AUTH_SERVER_URL=http://host.docker.internal:9082/o/check_api_key/
AUTH_SERVER_TOKENNAME=apiKey
AUTH_SERVER_CLIENTID=score
AUTH_SERVER_CLIENTSECRET=abc123
AUTH_SERVER_SCOPE_STUDY_PREFIX=score.
AUTH_SERVER_SCOPE_UPLOAD_SUFFIX=.WRITE
AUTH_SERVER_SCOPE_DOWNLOAD_SUFFIX=.READ
AUTH_SERVER_SCOPE_DOWNLOAD_SYSTEM=score.WRITE
AUTH_SERVER_SCOPE_UPLOAD_SYSTEM=score.READ


# ============================
# Song Integration (Required)
# ============================
METADATA_URL=http://host.docker.internal:8080



# ============================
# Storage Integration (Required)
# ============================
S3_ENDPOINT=http://localhost:9000/
S3_ACCESSKEY=minio
S3_SECRETKEY=minio123
S3_SIGV4ENABLED=true
OBJECT_SENTINEL=heliograph
BUCKET_NAME_OBJECT=oicr.icgc.test
BUCKET_NAME_STATE=oicr.icgc.test
COLLABORATORY_DATA_DIRECTORY=data
UPLOAD_PARTSIZE=1073741824
UPLOAD_CONNECTION_TIMEOUT=1200000
```

Run the following command:

```bash
docker run --name score --env-file .env.score -d -p   8087:8087  ghcr.io/overture-stack/score-server:47f006ce
```

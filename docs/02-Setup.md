# Setup

There are two ways to set up a score-server in a development environment:
​
- Or in a **[Docker environment](#run-as-a-container)** 
- As a **[Standalone Server](#run-as-a-standalone-server)** (requiring setup of dependent services)

Both will require you to clone the Song repository to your local computer:

```bash
git clone https://github.com/overture-stack/score.git
```

## Run as a Container

Several _make_ targets are provided for locally deploying the dependent services using Docker. As the developer, you can replicate a live environment for the `score-server` and `score-client` while developing locally. 

For more information on the different targets, run `make help` or read the comments above each target for a description.

### Prerequisites

- We reccommend Docker (version 4.32.0 or higher)
- You will need an internet connection for the _make_ command, which may take several minutes to build. 

### Starting the Server

#### For M1 Mac Systems

On a Mac M1 you must set the Docker BuildKit environment variable to the legacy builder.
​
```bash
DOCKER_BUILDKIT=0 make clean start-score-server
```

#### For all other Systems

To start song-server and all dependencies, use the following command:
​
```bash
make clean start-score-server
```

### Stopping the Server

To clean everything, including killing all services, maven cleaning, and removing generated files/directories, use the following command:

```bash
make clean
```

**Warning:** Docker for Score is meant to demonstrate the configuration and usage of Score, and is **_not intended for production_**. If you ignore this warning and use this in any public or production environment, please remember to change the passwords, accessKeys, and secretKeys.

## Run as a Standalone Server

### Prerequisites

- [JDK11](https://www.oracle.com/ca-en/java/technologies/downloads/) and [Maven3](https://maven.apache.org/download.cgi) are required to set up this service from source. 

### Building the Server

​To build the score-server run the following command from the Score directory:

```bash
./mvnw clean install -DskipTests
```

### Starting the Server

Before running your score-server, ensure that your local machine is connected and running the following dependent services:

- A Song Server connected to a database
- An Object Storage Service (S3 compatible storage or Azure)
- Authentication/Authorization Service (Keycloak)

Set the configuration of above dependent services on `score-server/src/main/resources/application.yml` the following profiles are available to you:

**Profiles**
| Profile | Description |
| - | - |
| `default` | Required to load default controllers |
| `secure` | Required to load security configuration |
| `s3` or `azure` | Required to choose between S3 compatible or Azure storage |
| `dev` | (Optional) to facilitate dev default configuration |

Run the following command to start the score-server:

```bash
cd score-server/
mvn spring-boot:run -Dspring-boot.run.profiles=default,s3,secure,dev
```

**Warning:** This guide is meant to demonstrate the configuration and usage of Score for development purposes and is not intended for production. If you ignore this warning and use this in any public or production environment, please remember to use Spring profiles accordingly. For production do not use ***dev*** profile.

## Configure with Keycloak

[Keycloak](https://www.keycloak.org/) is an open-source identity and access management solution that can be used to manage users and application permissions. You can find basic information on integrating Score and Keycloak using docker from our user docs [located here](https://www.overture.bio/documentation/score/docker-install/configuration/authentication/). For a comprehensive guide on installing and configuring Keycloak, refer to the [Keycloak documentation](https://www.keycloak.org/documentation).

### For a standalone server

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

### For Docker

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
```

Create a `.env.score` file with the necessary environment variables:

```bash
# ============================
# Spring Run Profiles (Required)
# ============================

# Active profiles to determine app behavior & configs
SPRING_PROFILES_ACTIVE=s3,prod,secure

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
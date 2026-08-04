# Setup

## Prerequisites

Before you begin, ensure you have the following installed on your system:

- [JDK11](https://www.oracle.com/ca-en/java/technologies/downloads/)
- [Docker](https://www.docker.com/products/docker-desktop/) (v4.39.0 or higher)

## Score-Server Development Setup

This guide will walk you through setting up a complete development environment, including Score and its complementary services.

### Setting up supporting services

The Score repository ships its own `docker-compose.yml` and `Makefile`, which together start every service Score depends on. No other repository is required.

1. Clone Score and move into its directory:

   ```bash
   git clone https://github.com/overture-stack/score.git
   cd score
   ```

2. Start Score's dependencies:

   ```bash
   make start-deps
   ```

   <details>
   <summary>**Click here for a detailed breakdown**</summary>

   `make start-deps` packages the project and then brings up Keycloak, Song, and object storage from the repository's `docker-compose.yml`:

   | Service     | Port    | Description                                     | Purpose in Score Development                |
   | ----------- | ------- | ----------------------------------------------- | ------------------------------------------- |
   | Keycloak    | `9082`  | Authorization and authentication service        | Provides OAuth2 authentication for Score    |
   | Keycloak-db | `9444`  | Database for Keycloak                           | Stores Keycloak data for authentication     |
   | Song        | `8080`  | Metadata management service                     | Manages metadata for files stored by Score  |
   | Song-db     | `12345` | Database for Song                               | Stores metadata managed by Song             |
   | Minio       | `8085`  | Object storage provider                         | Simulates S3-compatible storage for Score   |

   Keycloak starts with the `myrealm` realm imported from `docker/keycloak-init/data_import`, and downloads the `keycloak-apikeys` provider on start-up so it can issue API keys. The Song server is a pinned prebuilt image rather than a local build.

   To bring up Score itself along with all of the above, use `make start-score-server` instead. That adds:

   | Service      | Port           | Description      | Purpose in Score Development                                 |
   | ------------ | -------------- | ---------------- | ------------------------------------------------------------ |
   | Score-server | `8087`, `5006` | The Score server | The service under development; `5006` is the JVM debug port   |

   - Ensure these ports are free on your system before starting the environment.
   - You may need to adjust the ports in the `docker-compose.yml` file if you have conflicts with existing services.
   - `make clean` tears the stack down and removes the build output; `make log-score-server` tails the server's logs.

   :::note

   These targets build the project with the bundled Maven wrapper and drive Docker Compose, so a JDK is required even when you only want the supporting services. See the prerequisites above.

   :::

   </details>

### Running the Development Server

Use these steps to run Score on your host, against the supporting services started above. To run Score in a container instead, `make start-score-server` covers both.

1.  Build the application locally:

    ```bash
    ./mvnw clean install -DskipTests
    ```

     <details>
     <summary>**Click here for an explaination of command above**</summary>

    - `./mvnw`: This is the Maven wrapper script, which ensures you're using the correct version of Maven.
    - `clean`: This removes any previously compiled files.
    - `install`: This compiles the project, runs tests, and installs the package into your local Maven repository.
    - `-DskipTests`: This flag skips running tests during the build process to speed things up.

     </details>

    :::tip
    Ensure you are running JDK11. To check, you can run `java --version`. You should see something similar to the following:

    ```bash
    openjdk version "11.0.18" 2023-01-17 LTS
    OpenJDK Runtime Environment Corretto-11.0.18.10.1 (build 11.0.18+10-LTS)
    OpenJDK 64-Bit Server VM Corretto-11.0.18.10.1 (build 11.0.18+10-LTS, mixed mode)
    ```

    :::

2.  Start the Score Server:

    ```bash
    ./mvnw spring-boot:run -Dspring-boot.run.profiles=default,s3,secure,dev -pl score-server
    ```

    :::info

         If you are looking to configure Score for your specific environment, [**the Score-servers configuration file can be found here**](https://github.com/overture-stack/score/blob/develop/score-server/src/main/resources/application.yml). A summary of the available Spring profiles is provided below:

         <details>
         <summary>**Click here for a summary of the Score-server spring profiles**</summary>

         **Score Profiles**
         | Profile     | Description                                                                                                                     |
         | ----------- | ------------------------------------------------------------------------------------------------------------------------------- |
         | `default`   | Common settings for all environments. Includes server, S3, bucket, object, upload, and authentication configurations.           |
         | `ssl`       | Enables SSL configuration for using a self-signed certificate in production deployments.                                        |
         | `azure`     | Configuration for Azure blob storage. Includes Azure-specific settings and bucket policies.                                     |
         | `s3`        | Configuration for Amazon S3 or S3-compatible storage. Includes endpoint, access key, and secret key settings.                   |
         | `prod`      | Production environment configuration. Enables secure S3 connections and sets the metadata URL.                                  |
         | `secure`    | Security configuration for OAuth2 and JWT. Includes settings for resource server, authentication server, and scope definitions. |
         | `dev`       | Development environment configuration. Uses non-secure S3 connections, local endpoints, and disables upload cleaning.           |
         | `benchmark` | Configuration for benchmarking purposes. Includes SSL settings and a non-secure S3 endpoint.                                    |

         </details>

    :::

### Verification

After installing and configuring Score, verify that the system is functioning correctly:

1. **Check Server Health**

   ```bash
   curl -s -o /dev/null -w "%{http_code}" "http://localhost:8087/download/ping"
   ```

   - Expected result: Status code `200`
   - Troubleshooting:
     - Ensure Score server is running
     - Check you're using the correct port (default is 8087)
     - Verify no firewall issues are blocking the connection

2. **Check the Swagger UI**
   - Navigate to `http://localhost:8087/swagger-ui.html` in a web browser
   - Expected result: Swagger UI page with a list of available API endpoints
   - Troubleshooting:
     - Check browser console for error messages
     - Verify you're using the correct URL

:::info Need Help?
If you encounter any issues or have questions about our API, please don't hesitate to reach out through our [**support page**](https://docs.overture.bio/community/support) or our [**discussion forum**](https://github.com/overture-stack/docs/discussions?discussions_q=).
:::

## Score-Client Setup

The `score-client` is a CLI tool used for communicating with a `score-server`. For ease of deployment it can be run using Docker. The client can be configured through environment variables, which take precedence over the `application.yml` config.

```bash
docker run -d --name score-client \
 -e ACCESSTOKEN=<your-api-key> \
 -e STORAGE_URL=http://localhost:8087 \
 -e METADATA_URL=http://localhost:8080 \
 --network="host" \
 --platform="linux/amd64" \
 --mount type=bind,source=${pwd},target=/output \
 ghcr.io/overture-stack/score-client:latest
```

:::info Obtaining an API key

`ACCESSTOKEN` is environment-specific; there is no fixed development token. The Keycloak that `make start-deps` brings up on port `9082` loads the `keycloak-apikeys` provider, which issues keys against the `myrealm` realm. See [Authentication](/develop/Score/reference/authentication) for how the provider is installed and how Score validates the keys it issues.

<details>
<summary>**Click here for the steps to generate a key against the local stack**</summary>

The realm ships the users `admin` (a member of the `ADMIN` group) and `testca_user` (a member of `TESTCASONG_GROUP`), both with hashed passwords that are not recoverable from the realm export. Keys can only be issued by their owner or an administrator, so start by giving one of those users a password you know.

1. Open the Keycloak admin console at `http://localhost:9082` and sign in. The image's default administrator credentials are `user` / `bitnami`.

2. In the `myrealm` realm, set a password for the `admin` user (**Users** → `admin` → **Credentials**). Note its user ID from the same page; you will need it below.

3. Request a token for that user. The realm's `system` client has direct access grants enabled:

   ```bash
   curl -X POST "http://localhost:9082/realms/myrealm/protocol/openid-connect/token" \
     -d "grant_type=password" \
     -d "client_id=system" -d "client_secret=systemsecret" \
     -d "username=admin" -d "password=<the password you just set>"
   ```

4. Exchange that token for an API key, substituting the user ID from step 2:

   ```bash
   curl -X POST "http://localhost:9082/realms/myrealm/apikey/api_key?user_id=<user-id>&scopes=score.WRITE&scopes=score.READ" \
     -H "Authorization: Bearer <access_token from step 3>"
   ```

   The `name` field of the response is the key value. Pass it as `ACCESSTOKEN`:

   ```json
   {
     "name": "5b1da354-37bd-409d-b938-ea14b8035bc3",
     "scope": ["score.READ", "score.WRITE"],
     "expiryDate": "2027-07-30T15:32:59.990+0000",
     "isRevoked": false
   }
   ```

Scopes take the form `<resource>.<READ|WRITE>`, and the resources the realm defines are `song`, `score`, `TEST-CA`, and `ABC123`. A request for a scope the user's group does not carry is rejected with `Invalid Scope`.

</details>

:::

    <details>
    <summary>**Click here for an explaination of command above**</summary>
      - `-e ACCESSTOKEN=<your-api-key>` supplies the API key the score-client authenticates with, obtained from Keycloak as described above.
      - `-e STORAGE_URL=http://localhost:8087` is the url for the Score server that the Score-Client will interact with.
      - `-e METADATA_URL=http://localhost:8080` is the url for the song server that the score-client will interact with.
      - `--network="host"` Uses the host network stack inside the container, bypassing the usual network isolation. This means the container shares the network namespace with the host machine.
      - `--platform="linux/amd64"` Specifies the platform the container should emulate. In this case, it's set to linux/amd64, indicating the container is intended to run on a Linux system with an AMD64 architecture.
      - `--mount type=bind,source={pwd},target=/output` mounts the directory and its contents (volume) from the host machine to the container. In this case, it binds the present working directory from the host to /output inside the container. Any changes made to the files in this directory will be reflected in both locations.
    </details>

:::warning
This guide is meant to demonstrate the configuration and usage of Score for development purposes and is not intended for production. If you ignore this warning and use this in any public or production environment, please remember to use Spring profiles accordingly. For production do not use **dev** profile.
:::

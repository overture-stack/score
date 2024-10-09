# Setup

## Prerequisites

Before you begin, ensure you have the following installed on your system:
- [JDK11](https://www.oracle.com/ca-en/java/technologies/downloads/)
- [Docker](https://www.docker.com/products/docker-desktop/) (v4.32.0 or higher)

## Score-Server Development Setup

This guide will walk you through setting up a complete development environment, including Score and its complementary services.

### Setting up supporting services

We'll use our Conductor service, a flexible Docker Compose setup, to spin up Score's complementary services.

1. Clone the Conductor repository and move into its directory:

    ```bash
    git clone https://github.com/overture-stack/conductor.git
    cd conductor
    ```

2. Run the appropriate start command for your operating system:

    | Operating System | Command |
    |------------------|---------|
    | Unix/macOS       | `make scoreDev` |
    | Windows          | `make.bat scoreDev` |

    <details>
    <summary>**Click here for a detailed breakdown**</summary>

    This command will set up all complementary services for Score development as follows:

    ![ScoreDev](./assets/scoreDev.svg 'Score Dev Environment')

    | Service | Port | Description | Purpose in Score Development |
    |---------|------|-------------|------------------------------|
    | Conductor | `9204` | Orchestrates deployments and environment setups | Manages the overall development environment |
    | Keycloak-db | - | Database for Keycloak (no exposed port) | Stores Keycloak data for authentication |
    | Keycloak | `8180` | Authorization and authentication service | Provides OAuth2 authentication for Score |
    | Song-db | `5433` | Database for Song | Stores metadata managed by Song |
    | Song | `8080` | Metadata management service | Manages metadata for files stored by Score |
    | Minio | `9000` | Object storage provider | Simulates S3-compatible storage for Score |

    - Ensure all ports are free on your system before starting the environment.
    - You may need to adjust the ports in the `docker-compose.yml` file if you have conflicts with existing services.

    For more information, see our [Conductor documentation linked here](/docs/other-software/Conductor)

    </details>

### Running the Development Server 

1. Clone Score and move into its directory:

    ```bash
    git clone https://github.com/overture-stack/score.git
    cd score
    ```

2. Build the application locally:

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

3. Start the Score Server:

   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=default,s3,secure,dev -pl score-server
   ```

    :::info

        If you are looking to configure Score for your specific environment, [**the Score-servers configuration file can be found here**](https://github.com/overture-stack/score/blob/develop/score-server/src/main/resources/application.yml). A summary of the available Spring profiles is provided below:

        <details>
        <summary>**Click here for a summary of the Score-server spring profiles**</summary>

        **Score Profiles**
        | Profile | Description |
        |---------|-------------|
        | `default` | Common settings for all environments. Includes server, S3, bucket, object, upload, and authentication configurations. |
        | `ssl` | Enables SSL configuration for using a self-signed certificate in production deployments. |
        | `azure` | Configuration for Azure blob storage. Includes Azure-specific settings and bucket policies. |
        | `s3` | Configuration for Amazon S3 or S3-compatible storage. Includes endpoint, access key, and secret key settings. |
        | `prod` | Production environment configuration. Enables secure S3 connections and sets the metadata URL. |
        | `secure` | Security configuration for OAuth2 and JWT. Includes settings for resource server, authentication server, and scope definitions. |
        | `dev` | Development environment configuration. Uses non-secure S3 connections, local endpoints, and disables upload cleaning. |
        | `benchmark` | Configuration for benchmarking purposes. Includes SSL settings and a non-secure S3 endpoint. |

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

For further assistance, [open an issue on GitHub](https://github.com/overture-stack/score/issues/new?assignees=&labels=&projects=&template=Feature_Requests.md).

## Score-Client Setup

The `score-client` is a CLI tool used for communicating with a `score-server`. For ease of deployment it can be run using Docker. The client can be configured through environment variables, which take precedence over the `application.yml` config.

   ```bash
docker run -d --name score-client \
    -e ACCESSTOKEN=68fb42b4-f1ed-4e8c-beab-3724b99fe528 \
    -e STORAGE_URL=http://localhost:8087 \
    -e METADATA_URL=http://localhost:8080 \
    --network="host" \
    --platform="linux/amd64" \
    --mount type=bind,source=${pwd},target=/output \
    ghcr.io/overture-stack/score:latest
   ```

    <details>
    <summary>**Click here for an explaination of command above**</summary>
      - `-e ACCESSTOKEN=68fb42b4-f1ed-4e8c-beab-3724b99fe528` sets up the score-client with a pre-configured system-wide access token that works with the conductor service setup.
      - `-e STORAGE_URL=http://score:8087` is the url for the Score server that the Score-Client will interact with.
      - `-e METADATA_URL=http://song:8080` is the url for the song server that the score-client will interact with.
      - `--network="host"` Uses the host network stack inside the container, bypassing the usual network isolation. This means the container shares the network namespace with the host machine.
      - `--platform="linux/amd64"` Specifies the platform the container should emulate. In this case, it's set to linux/amd64, indicating the container is intended to run on a Linux system with an AMD64 architecture.
      - `--mount type=bind,source={pwd},target=/output` mounts the directory and its contents (volume) from the host machine to the container. In this case, it binds the present working directory from the host to /output inside the container. Any changes made to the files in this directory will be reflected in both locations.
    </details>

:::warning
This guide is meant to demonstrate the configuration and usage of Score for development purposes and is not intended for production. If you ignore this warning and use this in any public or production environment, please remember to use Spring profiles accordingly. For production do not use **dev** profile.
:::


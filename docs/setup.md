# Setup

## Prerequisites

Before you begin, ensure you have the following installed on your system:
- [JDK11](https://www.oracle.com/ca-en/java/technologies/downloads/)
- [Maven3](https://maven.apache.org/download.cgi)

## Developer Setup

This guide will walk you through setting up a complete development environment, including Score and its complementary services.

### 1. Set up supporting services

We'll use our Conductor service, a flexible Docker Compose setup, to spin up Score's complementary services.

```bash
git clone https://github.com/overture-stack/conductor.git
cd conductor
```

Next, run the appropriate start command for your operating system:

| Operating System | Command |
|------------------|---------|
| Unix/macOS       | `make scoreDev` |
| Windows          | `make.bat scoreDev` |

<details>
<summary>**Click here for a detailed breakdown**</summary>

This command will set up all complementary services for Score development as follows:

![ScoreDev](./assets/scoreDev.svg 'Score Dev Environment')

| Service | Port | Description |
|------------------|---------|------------------|
| Conductor | `9204` | Orchestrates deployments and environment setups |
| Keycloak-db | - | Database for Keycloak (no exposed port) |
| Keycloak | `8180` | Authorization and authentication service |
| Song-db | - | Database for Song (no exposed port) |
| Song | `8080` | Metadata management service |
| Minio | `9000` | Object storage provider |

For more information, see our [Conductor documentation linked here](/docs/other-software/Conductor)

</details>

In the next steps, we will run a Score development server against these supporting services.

### 2. Run the Score Development Server 

Begin by cloning Score and moving into its directory:

```bash
git clone https://github.com/overture-stack/score.git
cd score
```

Build the application locally by running:

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
Ensure you are running JDK11 and Maven3. To check, you can run `mvn --version`. You should see something similar to the following:
```bash
Apache Maven 3.8.6 (84538c9988a25aec085021c365c560670ad80f63)
Maven home: /opt/maven
Java version: 11.0.18, vendor: Amazon.com Inc., runtime: /Users/{username}/.sdkman/candidates/java/11.0.18-amzn
Default locale: en_CA, platform encoding: UTF-8
OS name: "mac os x", version: "14.6.1", arch: "aarch64", family: "mac"
```
:::

To start the Score Server, change directories to the `score-server` and run the following command:

```bash
cd score-server/
mvn spring-boot:run -Dspring-boot.run.profiles=default,s3,secure,dev
```

<details>
<summary>**Click here for an explaination of command above**</summary>

- `mvn spring-boot:run` starts the Spring Boot application while `-Dspring-boot.run.profiles=default,s3,secure,dev` specifies which Spring profiles to activate. 
- Score Servers configuration file can be found in the Score repository [located here](https://github.com/overture-stack/score/blob/develop/score-server/src/main/resources/application.yml).
- A summary of the available profiles is provided below:

**Profiles**
| Profile | Description |
| - | - |
| `default` | Required to load common configurations |
| `secure` | Required to load security configuration |
| `s3` or `azure` | Required to choose between S3 compatible or Azure storage |
| `dev` | (Optional) to facilitate dev default configuration |
---

</details>

:::warning
This guide is meant to demonstrate the configuration and usage of Score for development purposes and is not intended for production. If you ignore this warning and use this in any public or production environment, please remember to use Spring profiles accordingly. For production do not use **dev** profile.
:::

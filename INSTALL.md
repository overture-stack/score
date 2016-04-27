# Installing the ICGC Storage System
An overview sentence goes here.

## Prerequisites
The storage system must be built using a Maven version between 3.0.3 and 3.2.5 (inclusive). [Maven Version Manager](http://mvnvm.org/) may prove useful.

## Build From Source
Clone and build the [dcc-storage](https://github.com/icgc-dcc/dcc-storage) project:
```
git clone git@github.com:icgc-dcc/dcc-storage.git
mvn
```

Once the project has been built, its artifacts can be used to run the storage system.

## Run the System
First start the auth-server, metadata-server, and storage-server. Then end users can use the system via the icgc-storage-client.

Start the auth-server:
```
java -jar target/dcc-auth-server-1.0.13-SNAPSHOT.jar \
--spring.profiles.active=dev,no_scope_validation \
--logging.file=/var/log/dcc/auth-server/auth-server.log \
--server.port=8443 --management.port=8543 \
--endpoints.jmx.domain=auth
```

Start the metadata-server:
```
java -jar target/dcc-metadata-server-0.0.16-SNAPSHOT.jar \
--spring.profiles.active=development,secure \
--server.port=8444 \
--management.port=8544 \
--logging.file=/var/log/dcc/metadata-server/metadata-server.log \
--endpoints.jmx.domain=metadata \
--auth.server.url=https://localhost:8443/oauth/check_token \
--auth.server.clientId=metadata \
--auth.server.clientsecret=pass \
--spring.data.mongodb.uri=mongodb://localhost:27017/dcc-metadata
```

Start the storage-server:
```
java -jar target/dcc-storage-server-1.0.14-SNAPSHOT.jar \
--spring.profiles.active=dev,secure,default \
--logging.file=/var/log/dcc/storage-server/storage-server.log \
--server.port=5431 \
--bucket.name.object=beni-dcc-storage-dev \
--bucket.name.state=beni-dcc-storage-dev \
--auth.server.url=https://localhost:8443/oauth/check_token \
--auth.server.clientId=storage \
--auth.server.clientsecret=pass \
--metadata.url=https://localhost:8444 \
--endpoints.jmx.domain=storage
```

## Using the System
The `icgc-storage-client` subproject will have built an archive containing the script that should be used for all end-user interactions with the storage system. Look for `icgc-storage-client/target/icgc-storage-client-*-SNAPSHOT.tar.gz` and distribute this to anybody who will be using the storage system.

The end user can extract the archive then get started:
```
tar xvf icgc-storage-client-*-SNAPSHOT.tar.gz
cd icgc-storage-client-*-SNAPSHOT.tar.gz
bin/icgc-storage-client help
```

## Managing Authentication Credentials
[Temporary] Users and scopes can be managed by directly modifying the auth-server database. This can be done with H2 tools and JLine2. Change to the directory containing database.mv.db and run the following:

```
java -cp /Users/benjaminran/.m2/repository/com/h2database/h2/1.4.181/h2-1.4.181.jar:/Users/benjaminran/dev/jline2/target/jline-2.15-SNAPSHOT.jar jline.console.internal.ConsoleRunner org.h2.tools.Shell
```

Use URL `jdbc:h2:./database`, default driver, user `sa`, and password `secret`.

To add a new user:
```
INSERT INTO USERS VALUES('beni','beni',TRUE);
INSERT INTO OAUTH_CLIENT_DETAILS VALUES('beni','','beni','collab.upload,collab.download,aws.upload,aws.download,id.create,portal.download,s3.upload,s3.download','password','','ROLE_MANAGEMENT',33333333,null,'{}','');
```
### Examples
Upload a file:
```
# Needs amendment: bin/icgc-storage-client upload --manifest manifest.txt --file data.bam
java -Dlogging.file=/var/log/dcc/storage-client/storage-client.log \
-Dmetadata.url=https://localhost:5444 \
-Dmetadata.ssl.enabled=false \
-Dstorage.url=http://localhost:5431 \
-DaccessToken=dea1b1ff-9b90-4304-83d5-c5b72bd87b4b \
-jar target/dcc-storage-client-1.0.14-SNAPSHOT.jar \
upload --manifest ~/tmp/manifest.txt
```

## Development Notes

### Storage System Components
- auth-server
  - grants access tokens when requested from authenticated principal
- metadata-server
  - controls metadata in mongo (e.g. what?)
- storage-server
  - handles actual s3 upload/download requests

### End User Storage Client
The end user is supposed to use the packaged icgc-storage-client, but then I can't override dcc-storage-client sysProps like metadata.url, accessToken, etc. For now I'm ust directly invoking dcc-storage-client.jar.

### Auth-server
Grants access tokens to users wishing to access storage system (e.g. upload, download, etc.)

Using Spring 'dev' profile, which stores (unencrypted) credentials in a local, embedded H2 db. The db is created on server startup by src/main/resources/sql/schema.sql.

Auth-server has aws.upload/aws.download scopes, but storage-client upload seems to require s3.upload/s3.download, which doesn't seem to be defined/recognized by auth-server.
Added scopes to OAUTH-CLIENT-DETAILS

### Logging
Install guide commands currently send all logs to /var/log/dcc/[auth-server|metadata-server|storage-server|storage-client].

### Miscellaneous
Using bucket beni-dcc-storage-dev (Oregon) for prototype

Debug integration test:
```
mvn -Dtest=org.icgc.dcc.storage.test.StorageIntegrationTest -Dmaven.surefire.debug test
```

Remote debug storage-server
```
java -agentlib:jdwp=transport=dt_socket,server=y,address=5005,suspend=y \
-jar target/dcc-storage-server-1.0.14-SNAPSHOT.jar \
--spring.profiles.active=dev,secure,default \
--logging.file=/var/log/dcc/storage-server/storage-server.log \
--server.port=5431 \
--bucket.name.object=beni-dcc-storage-dev \
--bucket.name.state=beni-dcc-storage-dev \
--auth.server.url=https://localhost:8443/oauth/check_token \
--auth.server.clientId=storage \
--auth.server.clientsecret=pass \
--metadata.url=https://localhost:8444 \
--endpoints.jmx.domain=storage
```

### Questions
- Where is the gnosId created?
- What is the metadata in the metadata-server?
- What is bucket.state (storage-server systemProp)
- What is --endpoints.jmx.domain property for? (check application.yaml's)
- Where are scopes defined?

### Stack Traces
TODO

### To-Do
- Integrate successfully with S3
- Explore dcc-storage/dcc-storage-server/src/main/bin/install and dcc-storage-server scripts
- Find good way to specify dcc-storage-client java systemProps via icgc-storage-client that won't be interpreted as args to the application
- ...
- Rewrite auth-server

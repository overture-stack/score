# Installation Guide
Installing the ICGC Storage System


# Storage System Overview
Backend Components:
- storage-server: allows authenticated users to interact with entities in the storage system
- metadata-server: allows authenticated users to register entities with the storage system
- auth-server: authenticates users by granting access tokens via a REST api

Client Components:
- metadata-client: registers entities with the storage system
- storage-client: primary client for interaction with storage system

Each component is a Spring Boot java application packaged in a jar. Look in src/main/resources/application.yml for configuration properties, which can be overridden by specifying java system properties when running the jar. Configuration can also be set by adding an application.properties file via -Dspring.config.location.


# Installation
This guide describes setting up the ICGC Storage System on a single Ubuntu EC2 instance.

Before getting started:
- Make an S3 bucket to hold the storage system datal
- Make a KMS Master Key to encrypt data stored in S3 using the web console (http://docs.aws.amazon.com/kms/latest/developerguide/create-keys.html).
- Make an IAM role with permission to write to s3 (AmazonS3FullAccess).
- Launch an Ubuntu EC2 with the newly created IAM role and a static IP.
- Open ports 8444 and 5431 of the EC2 for anybody who will use the storage system as a client and open port 8443 to anybody who will generate access tokens to be given to end users.
  - Also, open ports 8443, 8444, 5431, and 27017 of the EC2 to the ip of the EC2 to ensure the servers can communicate with each other.
- Get a domain and point it towards the new EC2's IP address. The command shown in this guide use the domain storage.ucsc-cgl.org; this should be replaced with the desired domain.

Then do the following on the EC2:

Oracle Java is required.
```
# install oracle java
sudo apt-get install python-software-properties
sudo add-apt-repository ppa:webupd8team/java
sudo apt-get update
sudo apt-get install oracle-java8-installer
```

Add $DCC_HOME environment variable that points to the directory to hold all storage system files.
```
# set up $DCC_HOME
mkdir ~/dcc
printf "# ICGC Storage System\nexport DCC_HOME=~/dcc\n" >> ~/.bashrc
source ~/.bashrc
# add conf directories
mkdir $DCC_HOME/conf
mkdir $DCC_HOME/conf/ssl
mkdir $DCC_HOME/conf/maven
```

Maven version must be between 3.0.3 and 3.2.5 (inclusive).
```
# install mvnvm (http://mvnvm.org/)
curl -s https://bitbucket.org/mjensen/mvnvm/raw/master/mvn > $DCC_HOME/conf/maven/mvn
chmod 0755 $DCC_HOME/conf/maven/mvn
sudo ln -s $DCC_HOME/conf/maven/mvn /usr/local/bin/mvn
echo "mvn_version=3.2.5” >$DCC_HOME/conf/maven/mvnpvm.properties
```

Also, install unzip if it's not already installed.
```
sudo apt-get install unzip
```

Create an SSL certificate to be used across the storage system. This can be done using the LetsEncrypt certbot client (Note: this will require temporarily opening access to port 443 on the EC2). The root account may need to be used for some of this.
```
git clone https://github.com/certbot/certbot
cd certbot
./certbot-auto certonly --standalone --email benjaminran@ucsc.edu -d storage.ucsc-cgl.org
cd /etc/letsencrypt/archive/storage.ucsc-cgl.org/ # or wherever output from the previous command points you
# convert pem files to pkcs12
openssl pkcs12 -export -in cert1.pem -inkey privkey1.pem -out ucsc-storage.p12 -name tomcat -CAfile chain1.pem -caname root -chain
# convert pkcs12 to jks
keytool -importkeystore -destkeystore ucsc-storage.jks -deststorepass password -srckeystore ucsc-storage.p12 -srcstoretype PKCS12 -srcstorepass password
chown ubuntu:ubuntu ucsc-storage.p12
chown ubuntu:ubuntu ucsc-storage.jks
mv ucsc-storage.p12 ucsc-storage.jks $DCC_HOME/conf/ssl
```

The LetsEncrypt root CA certificate has to be added to the JVM truststore to tell the JVM to trust our newly generated certificate. To avoid altering the original, I make a copy that can be specified upon invocation of java clients.
```
# create copy of jvm truststore with LetsEncrypt cert added
cp /usr/lib/jvm/java-8-oracle/jre/lib/security/cacerts $DCC_HOME/conf/ssl/
keytool -import -file chain1.pem -alias LetsEncryptCA -keystore cacerts -storepass changeit
```

Install and configure MongoDB metadata-server dependency (https://docs.mongodb.com/manual/tutorial/install-mongodb-on-ubuntu/).
```
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv EA312927
echo "deb http://repo.mongodb.org/apt/ubuntu trusty/mongodb-org/3.2 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-3.2.list
sudo apt-get update
sudo apt-get install -y mongodb-org
```

Pull in and build the storage system source, linking to the ssl certificate while you're at it.
```
# clone storage system source
cd $DCC_HOME
git clone git@github.com:BD2KGenomics/dcc-storage.git
git clone git@github.com:BD2KGenomics/dcc-auth.git
git clone git@github.com:BD2KGenomics/dcc-metadata.git
# link mvnvm.properties and ssl certificate then build
for f in $DCC_HOME/dcc-*; do ln -s $DCC_HOME/conf/maven/mvnvm.properties $f/mvnvm.properties && ln -s $DCC_HOME/conf/ssl/ucsc-storage.jks $f/ucsc-storage.jks && cd $f && mvn; done;
```

Run the system.
```
# run the auth-server (TODO: no description of config properties file)
cd $DCC_HOME/dcc-auth/dcc-auth-server/ && java -Dspring.config.location=$DCC_HOME/conf/conf.old/ucsc-auth-server.properties -jar $DCC_HOME/dcc-auth/dcc-auth-server/target/dcc-auth-server-1.0.13-SNAPSHOT.jar
# run the metadata-server
cd $DCC_HOME/dcc-metadata/dcc-metadata-server && java -Djavax.net.ssl.trustStore=$DCC_HOME/conf/ssl/cacerts -Djavax.net.ssl.trustStorePassword=changeit -Dspring.profiles.active=development,secure -Dserver.port=8444 -Dmanagement.port=8544 -Dlogging.file=/var/log/dcc/metadata-server/metadata-server.log -Dauth.server.url=https://storage.ucsc-cgl.org:8443/oauth/check_token -Dauth.server.clientId=metadata -Dauth.server.clientsecret=pass -Dspring.data.mongodb.uri=mongodb://localhost:27017/dcc-metadata -Dserver.ssl.key-store=ucsc-storage.jks -Dserver.ssl.key-store-password=password -Dserver.ssl.key-store-type=JKS -jar target/dcc-metadata-server-0.0.16-SNAPSHOT.jar
# run the storage-server
cd $DCC_HOME/dcc-storage/dcc-storage-server && java -Djavax.net.ssl.trustStore=$DCC_HOME/conf/ssl/cacerts -Djavax.net.ssl.trustStorePassword=changeit -Dspring.profiles.active=secure,default -Dlogging.file=/var/log/dcc/storage-server/storage-server.log -Dserver.port=5431 -Dbucket.name.object=icgc-storage-prototype -Dbucket.name.state=icgc-storage-prototype -Dauth.server.url=https://storage.ucsc-cgl.org:8443/oauth/check_token -Dauth.server.clientId=storage -Dauth.server.clientsecret=pass -Dmetadata.url=https://storage.ucsc-cgl.org:8444 -Dendpoints.jmx.domain=storage -Ds3.endpoint=https://s3.amazonaws.com -Ds3.accessKey=foo -Ds3.secretKey=bar -Ds3.masterEncryptionKeyId=baz -Ds3.secured=true -Dupload.clean.enabled=false -Dserver.ssl.key-store=ucsc-storage.jks -Dserver.ssl.key-store-password=password -Dserver.ssl.key-store-type=JKS -jar target/dcc-storage-server-1.0.14-SNAPSHOT.jar
```


# Configuration
Each storage system component has an `application.yml` configuration file in the codebase whose properties can be overridden using Spring Boot mechanisms such as specifying Java system properties when running the jar or by specifying an external config file (i.e. `java -Dspring.config.location=/path/to/config.properties`).

The various configuration properties used by each component are documented next.


## auth-server
Auth-server Configuration Profiles and Properties


### Profiles
Auth-server Configuration Profiles


##### dev
Uses a local database of users and their credentials and scopes.


##### prod
NOT FOR NON-ICGC USE. Uses the ICGC production auth database.


##### no_scope_validation
Disables scope validation; any valid user in the auth database can access/do anything with any of the storage system data


### Properties
Auth-server Configuration Properties


##### management.security.role
e.g. SUPERUSER


##### management.context_path
e.g. /admin


##### management.address
e.g. 127.0.0.1


##### management.port
The port on which the management server listens
e.g. 8444


##### security.user.name


##### security.user.password


##### server.port
Port on which auth server listens for normal requests. See the [Spring Documentation](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto-change-the-http-port) for details.
e.g. 8443


##### server.ssl.key-store
Specifies the relative path of the keystore to use. See the [Spring Documentation](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto-configure-ssl) for details.
e.g. ssl-conf/


##### server.ssl.key-store-password
Specifies the password to the keystore file specified by server.ssl.key-store. See the [Spring Documentation](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto-configure-ssl) for details.


##### server.ssl.key-store-type
Specifies the keystore type (JKS is recommended). See the [Spring Documentation](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto-configure-ssl) for details.
e.g. JKS
e.g. PKCS12


##### database.file
Specifies the path to the local H2 database file that will be created if the "dev" profile is enabled.
e.g. ./target/data/database


##### database.backupDir
Specifies the directory where backups should be placed when a backup is triggered via the auth server management interface backup endpoint. See the [Spring Documentation](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-sql) for details.
e.g. /tmp/backups


##### spring.datasource.url
Path to database file.
e.g. jdbc:h2:/path/to/database;AUTO_SERVER=TURE


##### spring.datasource.userName
Username for accessing database.


##### spring.datasource.password
Password for accessing database.


##### spring.datasource.initialize
TODO
e.g. true


##### spring.datasource.schema
SQL script to execute on database intialization.
e.g. "classpath:sql/auth-schema.sql"


## metadata-server
TODO


## storage-server


## metadata-client
TODO


## storage-client
TODO


# Storage System Client Usage
A temporary client is in use; the client is a tar.gz archive containing a packaged combination of the two ICGC clients (storage-client and metadata-client) along with a few small wrapper scripts and ssl configuration. ˜
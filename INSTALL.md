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

Each component is a Spring Boot java application packaged in a JAR. Look in src/main/resources/application.yml for default configuration properties, which can be overridden by specifying java system properties when running the jar or by adding an application.properties file via -Dspring.config.location.


# Installation
This guide describes setting up the ICGC Storage System on a single Ubuntu EC2 instance.

Before getting started:
- Ensure you have access to the dcc-auth, dcc-metadata, and dcc-storage source
- Make an S3 bucket to hold the storage system data
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
sudo apt-get install -y python-software-properties
sudo add-apt-repository ppa:webupd8team/java
sudo apt-get update
sudo apt-get install -y oracle-java8-installer
```

Ruby is used.
```
sudo apt-get install ruby
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
echo "mvn_version=3.2.5" >$DCC_HOME/conf/maven/mvnvm.properties
```

Also, install unzip if it's not already installed.
```
sudo apt-get install -y unzip git
```

Create an SSL certificate to be used across the storage system. This can be done using the LetsEncrypt certbot client (**Note:** this will require temporarily opening access to port 443 on the EC2). The root account may need to be used for some of this.
```
git clone https://github.com/certbot/certbot
cd certbot
./certbot-auto certonly --standalone --email <administrator-email-address> -d storage.ucsc-cgl.org
cd /etc/letsencrypt/archive/storage.ucsc-cgl.org/ # or wherever output from the previous command points you
# convert pem files to pkcs12
openssl pkcs12 -export -in cert1.pem -inkey privkey1.pem -out ucsc-storage.p12 -name tomcat -CAfile chain1.pem -caname root -chain
# if you get an error "Error unable to get issuer certificate getting chain." see https://hardwarehacks.org/blogs/devops/2016/03/13/1457912280000.html 
# you'll need to do the following to add in the Letsencrypt root keys
wget --quiet https://letsencrypt.org/certs/isrgrootx1.pem
wget --quiet -O dstrootx3.pem https://ssl-tools.net/certificates/dac9024f54d8f6df94935fb1732638ca6ad77c13.pem
cat isrgrootx1.pem dstrootx3.pem chain1.pem > bundle.pem
openssl pkcs12 -export -in cert1.pem -inkey privkey1.pem -out ucsc-storage.p12 -name tomcat -CAfile bundle.pem -caname root -chain
# and use password for password
# convert pkcs12 to jks
keytool -importkeystore -destkeystore ucsc-storage.jks -deststorepass password -srckeystore ucsc-storage.p12 -srcstoretype PKCS12 -srcstorepass password
chown ubuntu:ubuntu ucsc-storage.p12
chown ubuntu:ubuntu ucsc-storage.jks
mv chain1.pem ucsc-storage.p12 ucsc-storage.jks $DCC_HOME/conf/ssl
```

The LetsEncrypt root CA certificate has to be added to the JVM truststore to tell the JVM to trust our newly generated certificate. To avoid altering the original, a copy is made that can be specified upon invocation of java clients.
```
# create copy of jvm truststore with LetsEncrypt cert added
cp /usr/lib/jvm/java-8-oracle/jre/lib/security/cacerts $DCC_HOME/conf/ssl/
keytool -import -file chain1.pem -alias LetsEncryptCA -keystore cacerts -storepass changeit
# if you used the bundle.pem above then use that in place of chain1.pem here
```

Install and configure MongoDB metadata-server dependency (https://docs.mongodb.com/manual/tutorial/install-mongodb-on-ubuntu/). The commands shown below leave access to mongodb unrestricted. The port that mongod listens on shouldn't be open to external IPs, and in production systems access restriction should be enabled.
```
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv EA312927
echo "deb http://repo.mongodb.org/apt/ubuntu trusty/mongodb-org/3.2 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-3.2.list
sudo apt-get update
sudo apt-get install -y mongodb-org
printf 'use admin\ndb.createUser({user:"%s",pwd:"%s", roles:[{role:"userAdminAnyDatabase",db:"admin"}]})' metadata pass | mongo
printf '\n# Enable auth\nauth=true\n' | sudo tee -a /etc/mongod.conf >/dev/null 2>&1
sudo service mongod restart
```

Install git.
```
sudo apt-get install -y git
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
cd $DCC_HOME/dcc-auth/dcc-auth-server/ && java -Dspring.profiles.active=dev,no_scope_validation -Dserver.ssl.key-store=ucsc-storage.jks -Dserver.ssl.key-store-password=password -Dserver.ssl.key-store-type=JKS -Dlogging.file=/var/log/dcc/auth-server/auth-server.log -Dserver.port=8443 -Dmanagement.port=8543 -jar $DCC_HOME/dcc-auth/dcc-auth-server/target/dcc-auth-server-1.0.13-SNAPSHOT.jar
# run the metadata-server
cd $DCC_HOME/dcc-metadata/dcc-metadata-server && java -Djavax.net.ssl.trustStore=$DCC_HOME/conf/ssl/cacerts -Djavax.net.ssl.trustStorePassword=changeit -Dspring.profiles.active=development,secure -Dserver.port=8444 -Dmanagement.port=8544 -Dlogging.file=/var/log/dcc/metadata-server/metadata-server.log -Dauth.server.url=https://storage.ucsc-cgl.org:8443/oauth/check_token -Dauth.server.clientId=metadata -Dauth.server.clientsecret=pass -Dspring.data.mongodb.uri=mongodb://localhost:27017/dcc-metadata -Dserver.ssl.key-store=ucsc-storage.jks -Dserver.ssl.key-store-password=password -Dserver.ssl.key-store-type=JKS -jar target/dcc-metadata-server-0.0.16-SNAPSHOT.jar
# run the storage-server
cd $DCC_HOME/dcc-storage/dcc-storage-server && java -Djavax.net.ssl.trustStore=$DCC_HOME/conf/ssl/cacerts -Djavax.net.ssl.trustStorePassword=changeit -Dspring.profiles.active=secure,default -Dlogging.file=/var/log/dcc/storage-server/storage-server.log -Dserver.port=5431 -Dbucket.name.object=<s3-bucket-name> -Dbucket.name.state=<s3-bucket-name> -Dauth.server.url=https://storage.ucsc-cgl.org:8443/oauth/check_token -Dauth.server.clientId=storage -Dauth.server.clientsecret=pass -Dmetadata.url=https://storage.ucsc-cgl.org:8444 -Dendpoints.jmx.domain=storage -Ds3.endpoint=https://s3.amazonaws.com -Ds3.accessKey=foo -Ds3.secretKey=bar -Ds3.masterEncryptionKeyId=baz -Ds3.secured=true -Dupload.clean.enabled=false -Dserver.ssl.key-store=ucsc-storage.jks -Dserver.ssl.key-store-password=password -Dserver.ssl.key-store-type=JKS -jar target/dcc-storage-server-1.0.14-SNAPSHOT.jar
```
Note: passwords (and ideally all configuration) should be specified in configuration files in production systems.

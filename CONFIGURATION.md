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
Specifies the relative path of the keystore to use. HTTPS will be served implicitly. See the [Spring Documentation](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto-configure-ssl) for details.

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
Metadata Server Configuration Profiles and Properties


### Profiles
Metadata Server Configuration Profiles


##### development


##### secure


### Properties
Metadata Server Configuration Properties


##### server.port
Port number that server will listen on

e.g. 8444


##### management.port
Port number that management server will listen on

e.g. 8544


##### server.ssl.key-store
Specifies the relative path of the keystore to use. HTTPS will be served implicitly. See the [Spring Documentation](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto-configure-ssl) for details.

e.g. ssl-conf/


##### server.ssl.key-store-password
Specifies the password to the keystore file specified by server.ssl.key-store. See the [Spring Documentation](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto-configure-ssl) for details.


##### server.ssl.key-store-type
Specifies the keystore type (JKS is recommended). See the [Spring Documentation](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto-configure-ssl) for details.

e.g. JKS

e.g. PKCS12


##### logging.file


##### auth.server.url
URL where auth-server is listening

e.g. https://storage.ucsc-cgl.org:8443


##### auth.server.clientId
Principle name that metadata-server should use to authenticate to auth-server

e.g. metadata


##### auth.server.clientSecret
Password that metadata-server should use to authenticate to auth-server

e.g. password


##### spring.data.mongodb.uri
URI where mongodb is running with dcc-metadata database

e.g. mongodb://localhost:27017/dcc-metadata


## storage-server
Storage Server Configuration Profiles and Properties


### Profiles
Storage Server Configuration Profiles


##### secure


##### [default]


### Properties
Storage Server Configuration Properties


##### logging.file
e.g. /var/log/dcc/storage-server/storage-server.log


##### server.port
Port number that server shold listen on.

e.g. 5431


##### auth.server.url
URL where auth-server is listening

e.g. https://storage.ucsc-cgl.org:8443


##### auth.server.clientId
Principle name that storage-server should use to authenticate to auth-server

e.g. storage


##### auth.server.clientSecret
Password that storage-server should use to authenticate to auth-server

e.g. password


##### metadata.url
URL where metadata-server is listening

e.g. https://storage.ucsc-cgl.org:8444


##### s3.endpoint
Endpoint URL used to connect to s3. See [AWS Documentation](http://docs.aws.amazon.com/general/latest/gr/rande.html#s3_region) for details.

e.g. https://s3-us-west-2.amazonaws.com (for bucket in Oregon)


##### s3.accessKey
AWS account access key


##### s3.secretKey
AWS account secret key


##### s3.masterEncryptionKeyId
KMS master key ID


##### s3.secured
Specifies whether or not https should be used when making requests to S3


##### upload.clean.enabled
TODO


##### server.ssl.key-store
Specifies the relative path of the keystore to use. HTTPS will be served implicitly. See the [Spring Documentation](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto-configure-ssl) for details.

e.g. ssl-conf/


##### server.ssl.key-store-password
Specifies the password to the keystore file specified by server.ssl.key-store. See the [Spring Documentation](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto-configure-ssl) for details.


##### server.ssl.key-store-type
Specifies the keystore type (JKS is recommended). See the [Spring Documentation](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto-configure-ssl) for details.

e.g. JKS

e.g. PKCS12


## metadata-client
TODO


## storage-client
TODO

# ICGC DCC - Storage Client

ICGC storage client for the ICGC storage system.

## Libraries

The storage client is comprised of the following components:

- [Spring Boot](http://projects.spring.io/spring-boot/)
- [Spring Security](http://projects.spring.io/spring-security/)
- [Spring Security OAuth](http://projects.spring.io/spring-security-oauth/)
- [AWS SDK](https://aws.amazon.com/sdk-for-java/)
- [JavaFS](https://github.com/puniverse/javafs)
- [HTSJDK](https://samtools.github.io/htsjdk/)

## Build

To compile, test and package the module, execute the following from the root of the repository:

```shell
mvn -am -pl score/score-core
```

## Run

```shell
bin/score-client upload —manifest manifest.txt
```

## Develop

The following will allow a developer to hit against production from the Eclipse IDE

### Entry Point

```shell
bio.overture.score.client.ClientMain
```

### VM Arguments

```shell
-Dspring.config.location=src/main/conf/
-Dspring.profiles.active=prod
-Dclient.strictSsl=true
-Dclient.upload.serviceHostname=<host>
-DaccessToken=<token>
```

### Program Arguments

```
score-client upload --manifest src/test/resources/fixtures/upload/manifest.txt
```

## HTTP Logging

To enable logging of request bodies and headers, append the following to the command line:

`--logging.level.org.apache.http=DEBUG`


# ICGC DCC - Storage Server

Storage server for ICGC storage system. 

## Libraries

The storage server is comprised of the following components:

- [Spring Boot](http://projects.spring.io/spring-boot/)
- [Spring Security](http://projects.spring.io/spring-security/)
- [Spring Security OAuth](http://projects.spring.io/spring-security-oauth/)
- [AWS SDK](https://aws.amazon.com/sdk-for-java/)

## Build

To compile, test and package the module, execute the following from the root of the repository:

```shell
mvn -am -pl dcc-storage/dcc-storage-fs
```

## Logging

To enable logging of request bodies and headers, append the following to the command line:

`--logging.level.org.apache.http=DEBUG`

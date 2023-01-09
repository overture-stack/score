# SCORe Client

SCORe Client for the SCORe storage system.

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
## Docker image Usage

An example usage of the container which will download a remote file (with associated index file) having `object-id` `5b845b9a-3dcd-59ef-9f56-9a99396e988f` to `/tmp` on the docker host machine in "bundle" layout. The files will be written with ownership set to the current user (`/usr/bin/id -u`) and group (`/usr/bin/id -g`)

```shell
# Get latest image
pull overture/score

# Publish token
export ACCESSTOKEN=<access token from https://dcc.icgc.org>

# Make life easy for usage
alias score-client="docker run -it --rm  -u $(id -u):$(id -g) -e ACCESSTOKEN -v /tmp:/data score-client 
bin/score-client"

# Usage with an example object-id from https://dcc.icgc.org
score-client download --object-id 5b845b9a-3dcd-59ef-9f56-9a99396e988f --output-dir /data --output-layout bundle
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

## Azure
For azure support, use the `azure` profile. Block sizes for uploads
can be configured with the `azure.blockSize` property. 
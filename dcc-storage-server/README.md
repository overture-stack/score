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
mvn -am -pl dcc-storage/dcc-storage-server
```

## Logging

To enable logging of request bodies and headers, append the following to the command line:

`--logging.level.org.apache.http=DEBUG`

## Azure

Experimental functionality supporting an Azure Blob Storage repository has been added. 

Spring profile: ``azure``

New configuration parameters include:

```yaml
azure:
  accountName: 
  accountKey: 

bucket:
  name.object: data
  policy.upload: UploadPolicy
  policy.download: DownloadPolicy
  
download:
  partsize: 250000000  # 250 MB
```
At this time, only a single Azure Blob Storage account (and container) is used. However, since Azure Storage can only store 500 TB per account, the Storage Server will need to manage multiple account/key credentials in the near future.  It may also make sense to have multiple containers per account as well. There are suggestions that having many objects in a single container can impose a performance penalty on some operations.

The Storage Server no longer uses ``.meta`` files to track state in the repository. Object Specifications are dynamically generated on the fly for use on the client (to allow downloads to be resumed). Also, the block upload implementation supplied by Microsoft in the Azure Java SDK supercedes the use of this file. 

For Azure downloads, the ``download.partsize`` property is used to determine the size of individual parts to download. Currently, this is 250 MB.




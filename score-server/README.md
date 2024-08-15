# SCORe - Storage Server

## Libraries

The storage server is comprised of the following components:

- [Spring Boot](http://projects.spring.io/spring-boot/)
- [Spring Security](http://projects.spring.io/spring-security/)
- [Spring Security OAuth](http://projects.spring.io/spring-security-oauth/)
- [AWS SDK](https://aws.amazon.com/sdk-for-java/)

## Build

To compile, test and package the module, execute the following from the root of the repository:

```shell
mvn -am -pl score/score-server
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
### S3 Configuration
To configure SCORe to use an S3-compatible object storage service, the s3 Spring profile must be activated. This profile is designed to work with services like AWS S3, MinIO, or any other compatible service. Below are the configuration properties available under the s3 section in application.yml, along with their descriptions:

Profile Name: ``s3``

# Configuration Properties


s3.secured
Description: Determines whether the connection to the S3 service should use HTTPS (true) or HTTP (false). Set to true to secure the connection.
s3.endpoint
Description: The URL of the S3-compatible service. This is the endpoint where the service is hosted (e.g., s3.amazonaws.com for AWS S3 or a custom URL for MinIO). This property should be provided based on the service you're using.
s3.accessKey
Description: The access key for authenticating with the S3 service. It's required for secure access and should be kept confidential.
s3.secretKey
Description: The secret key paired with the access key for secure authentication to the S3 service. It must also be kept confidential.
s3.masterEncryptionKeyId
Description: The ID of the encryption key used for server-side encryption of files stored in S3. If provided, this key ensures that all data at rest is encrypted using the specified key.
s3.customMd5Property
Description:  A custom metadata property that stores an MD5 checksum of the uploaded files. This is useful for validating the integrity of the files.
s3.connectionTimeout
Description: The maximum amount of time (in milliseconds) the client will wait to establish a connection to the S3 service before timing out. This helps manage delays in the network or service availability.
s3.retryLimit
Description: The number of retries the client will attempt if an operation fails (e.g., an upload). This helps ensure robustness in case of transient issues.
s3.sigV4Enabled
Description: Enables AWS Signature Version 4 for request signing. This is required for certain regions or when using advanced features like server-side encryption with AWS KMS.

### At this time, only a single Azure Blob Storage account (and container) is used. However, since Azure Storage can only store 500 TB per account, the Storage Server will need to manage multiple account/key credentials in the near future.  It may also make sense to have multiple containers per account as well. There are suggestions that having many objects in a single container can impose a performance penalty on some operations.

The Storage Server no longer uses ``.meta`` files to track state in the repository. Object Specifications are dynamically generated on the fly for use on the client (to allow downloads to be resumed). Also, the block upload implementation supplied by Microsoft in the Azure Java SDK supercedes the use of this file. 

For Azure downloads, the ``download.partsize`` property is used to determine the size of individual parts to download. Currently, this is 250 MB.




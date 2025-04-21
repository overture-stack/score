# Score

Score is a file transfer service designed to enable large-file upload and download, providing a robust API for secure file transfer and storage operations. It serves as an intermediary between object storage systems and user authorization mechanisms, using pre-signed URLs for efficient and protected data access.

</br>

> 
> <div>
> <img align="left" src="ov-logo.png" height="50"/>
> </div>
> 
> *Score is part of [Overture](https://www.overture.bio/), a collection of open-source software microservices used to create platforms for researchers to organize and share genomics data.*
> 

## Key Features

- **Multi-cloud Support**: Compatible with AWS S3, Azure Storage, and any object storage with an S3 compliant API (Minio, Ceph, etc.)
- **High-performance Transfers**: Implements multipart uploads and downloads for optimal throughput
- **Genomic Data Handling (SamTools)**: Supports BAM/CRAM file slicing by genomic region and provides built-in samtools operations for BAM file handling
- **Data Integrity**: Ensures file integrity through MD5 checksum validation on uploads and downloads
- **Security**: Implements ACL-based security using OAuth2 with study code-scoped access
- **Metadata Integration**: Integrates with the Song metadata management system for comprehensive data tracking
- **File Bundling**: Enables efficient transfer of multiple files in a single bundle
- **Resumable Downloads**: Supports resuming downloads after network interruptions
- **FUSE Support**: Offers file system in Userspace (FUSE) support for enhanced file operations
- **Interactive API Documentation:** Built-in Swagger UI for easy API interaction and exploration

## Documentation

Technical resources for those working with or contributing to the project are available from our official documentation site, the following content can also be read and updated within the `/docs` folder of this repository.

- **[Score Overview](https://docs.overture.bio/docs/core-software/Score/overview)** 
- [**Setting up the Development Enviornment**](https://docs.overture.bio/docs/core-software/Score/setup)
- [**Common Usage Docs**](https://docs.overture.bio/docs/core-software/Score/setup)

##  Development Environment

- [Java 11 (OpenJDK)](https://openjdk.java.net/projects/jdk/11/)
- [Maven 3.5+](https://maven.apache.org/) (or use provided wrapper)
- [VS Code](https://code.visualstudio.com/) or preferred Java IDE
- [Docker](https://www.docker.com/) Container platform

## Support & Contributions

- For support, feature requests, and bug reports, please see our [Support Guide](https://docs.overture.bio/community/support).
- For detailed information on how to contribute to this project, please see our [Contributing Guide](https://docs.overture.bio/docs/contribution).

## Related Software 

The Overture Platform includes the following Overture Components:

</br>

|Software|Description|
|---|---|
|[Score](https://github.com/overture-stack/score/)| Transfer data to and from any cloud-based storage system |
|[Song](https://github.com/overture-stack/song/)| Catalog and manage metadata associated to file data spread across cloud storage systems |
|[Maestro](https://github.com/overture-stack/maestro/)| Organizing your distributed data into a centralized Elasticsearch index |
|[Arranger](https://github.com/overture-stack/arranger/)| A search API with reusable search UI components |
|[Stage](https://github.com/overture-stack/stage)| A React-based web portal scaffolding |
|[Lyric](https://github.com/overture-stack/lyric)| A model-agnostic, tabular data submission system |
|[Lectern](https://github.com/overture-stack/lectern)| Schema Manager, designed to validate, store, and manage collections of data dictionaries.  |

If you'd like to get started using our platform [check out our quickstart guides](https://docs.overture.bio/guides/getting-started)

## Funding Acknowledgement

Overture is supported by grant #U24CA253529 from the National Cancer Institute at the US National Institutes of Health, and additional funding from Genome Canada, the Canada Foundation for Innovation, the Canadian Institutes of Health Research, Canarie, and the Ontario Institute for Cancer Research.

#### Powered by
[![JetBrains logo.](https://resources.jetbrains.com/storage/products/company/brand/logos/jetbrains.svg)](https://jb.gg/OpenSourceSupport)

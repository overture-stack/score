# Overview

Score is a file transfer service designed to enable large-file upload and download, providign a robust API for secure file transfer and storage operations. It serves as an intermediary between object storage systems and user authorization mechanisms, using pre-signed URLs for efficient and protected data access.

## System Architecture

Score's primary function is to broker authenticated access to your object storage provider. It achieves this by:

1. Validating user access rights against an authorization system (OAuth)
2. Generating time-limited pre-signed URLs for object access
3. Facilitating secure data transfer between clients and object storage

![Score Arch](./assets/scoreDev.svg 'Score Architecture Diagram')

As part of the larger Overture.bio software suite, Score is typically used with multiple other services including:

- **Song:** A metadata management service made to manage file metadata independently from object storage concerns
- **Score Client:** A command line tool to streamline interactions with Scores REST API endpoints
- **Keycloak:** The authorization and authentication service used to provided OAuth2 authentication for Score


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


## Repository Structure
The repository is organized with the following directory structure:
```
.
├── /score-client
├── /score-core
├── /score-fs
├── /score-server
└── /score-test
```

- **score-client:** Command line app for uploading and downloading files, published as a [docker container](https://github.com/overture-stack/score/pkgs/container/score) and availabe as an executable jar from [github releases](https://github.com/overture-stack/score/releases)
- **score-core:** Core library containing shared utilities and data models used by all other packages
- **score-fs:** File system operations module for managing local files
- **score-server:** Main server application that handles object storage and transfers, published as a [docker container](https://github.com/overture-stack/score/pkgs/container/score-server)
- **score-test:** Integration and end-to-end test suite for all packages

# Score Developer Documentation

---

**Navigation**
- [Operation](./operation/operation.md)
- [Contribution](./contribution/contribution.md) 

---

# Background

Score's primary function is to facilitate the secure upload and download of file data to and from an object storage provider. Utilizing time-limited <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/ShareObjectPreSignedURL.html" target="_blank" rel="noopener noreferrer">pre-signed URLs</a>, Score ensures secure access to file data within object storage.

Score specializes in data transfer, leaving metadata complexities to its companion application, <a href="https://github.com/overture-stack/score" target="_blank" rel="noopener noreferrer">Song</a>. Song manages metadata validation and tracking, maintaining a separate repository from object storage. Together, Score and Song provide an efficient solution for distributed data organization.

## High Performance Transfers

Score offers a high-performance multipart transfer system with several advantages:

- Enables segmented file downloads, allowing for pause and resume functionality
- Automatically resumes file transfers if interrupted, for instance, due to connection issues
- Utilizes parallelization for efficient and rapid file uploads and downloads

## Data Integrity

- Score guarantees file transfer authenticity by conducting <a href="https://www.ietf.org/rfc/rfc1321.txt" target="_blank" rel="noopener noreferrer">MD5 validations</a> on all file uploads and downloads

## BAM & CRAM Slicing

- Score client incorporates <a href="http://www.htslib.org/" target="_blank" rel="noopener noreferrer">samtools</a> features, enabling viewing of reads from BAM files
- Provides the capability to slice BAM and CRAM files by genomic regions using command line tools

## The Score Client

The Score-Client, a command-line tool, simplifies interactions with Score's REST API endpoints. With Score-Client, users can efficiently upload and download files, and access various Score-related parameters. For detailed information on Score-Client commands, refer to our <a href="www.overture.bio/documentation/score/user-guide/commands/" target="_blank" rel="noopener noreferrer">score client reference documentation</a>.

---

**Navigation**

- [Operation](./operation/operation.md)
- [Contribution](./contribution/contribution.md) 

---
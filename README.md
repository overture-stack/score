# Score - File Transfer & Object Storage

[<img hspace="5" src="https://img.shields.io/badge/chat-on--slack-blue?style=for-the-badge">](http://slack.overture.bio)
[<img hspace="5" src="https://img.shields.io/badge/License-gpl--v3.0-blue?style=for-the-badge">](https://github.com/overture-stack/score/blob/develop/LICENSE)
[<img hspace="5" src="https://img.shields.io/badge/Code%20of%20Conduct-2.1-blue?style=for-the-badge">](code_of_conduct.md)

<div>
<img align="right" width="100vw" src="icon-score.png" alt="score-logo" hspace="30"/>
</div>

Genomics data volume and velocity have increased dramatically, rendering on-premise storage insufficient and demanding specialized software tools to manage data in the cloud. [Score](https://www.overture.bio/documentation/score/) addresses this by facilitating the transfer and storage of genomics data to and from a cloud network.

<!--Blockqoute-->

</br>

> 
> <div>
> <img align="left" src="ov-logo.png" height="90"/>
> </div>
> 
> *Score is a core component within the [Overture](https://www.overture.bio/) research software ecosystem. Overture is a toolkit of modular software components made to build into scalable genomics data management systems. See our [related products](#related-products) for more information on what Overture can offer.*
> 
> 
<!--Blockqoute-->

## Technical Specifications

- Written in JAVA 
- Supports AWS S3, Azure, Google Cloud, Openstack with Ceph, Minio and all other S3-compliant cloud storage solutions
- Built-in [Samtools](http://www.htslib.org/) functionality including BAM and CRAM file slicing by genomic region 
- ACL security using [OAuth 2.0](https://oauth.net/2/) and scopes based on study codes
- Multipart Uploads and Downloads
- REST API with [Swagger UI](https://swagger.io/tools/swagger-ui/)
- [MD5sum](https://www.intel.com/content/www/us/en/support/programmable/articles/000078103.html) validation

## Documentation

- :construction: Developer documentation, including instructions for running Score from source can be found in the [Wiki](https://github.com/overture-stack/score/wiki) :construction:
- For user documentation, including installation, configuration and usage guides, see the Overture websites [Score documentation page](https://www.overture.bio/documentation/score/)

## Support & Contributions

- Filing an [issue](https://github.com/overture-stack/score/issues)
- Making a [contribution](CONTRIBUTING.md)
- Connect with us on [Slack](http://slack.overture.bio)
- Add or Upvote a [feature request](https://github.com/overture-stack/score/issues?q=is%3Aopen+is%3Aissue+label%3Anew-feature+sort%3Areactions-%2B1-desc)

## Related Software 

<div>
  <img align="right" alt="Overture overview" src="https://www.overture.bio/static/124ca0fede460933c64fe4e50465b235/a6d66/system-diagram.png" width="45%" hspace="5">
</div>

Score commonly works in tandem with our metadata service, [Song](https://github.com/overture-stack/SONG). While Score handles object storage and file transfer, Song validates and tracks all the associated file metadata. 

All our core microservices are included in the Overture **Data Management System** (DMS). Built from our core collection of microservices, the DMS offers turnkey installation, configuration, and deployment of Overture software. For more information on the DMS, read our [DMS documentation](https://www.overture.bio/documentation/dms/).

See the links below for information on our other research software tools:

</br>

|Software|Description|
|---|---|
|[Ego](https://www.overture.bio/products/ego/)|An authorization and user management service|
|[Ego UI](https://www.overture.bio/products/ego-ui/)|A UI for managing Ego authentication and authorization services|
|[Score](https://www.overture.bio/products/score/)| Transfer data to and from any cloud-based storage system|
|[Song](https://www.overture.bio/products/song/)|Catalog and manage metadata associated to file data spread across cloud storage systems|
|[Maestro](https://www.overture.bio/products/maestro/)|Organizing your distributed data into a centralized Elasticsearch index|
|[Arranger](https://www.overture.bio/products/arranger/)|A search API with reusable UI components that build into configurable and functional data portals|
|[DMS-UI](https://github.com/overture-stack/dms-ui)|A simple web browser UI that integrates Ego and Arranger|
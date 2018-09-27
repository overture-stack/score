introduction.rst

==============
Introduction
==============


What is Score?
======================

`Score <https://www.overture.bio/products/score>`_ facilitates the transfer and storage of your data seamlessly for cloud-based projects. File bundling, resumable downloads, and BAM/CRAM slicing make data transfer fast and smooth.

The method with which Score facilitates the transfer of data is through the use of pre-signed URLs. As such, Score can be thought of as a broker between an object storage system and user authorization system, validating user access and generating signed URLs for object access. 


-----------------------------------------

.. _introduction_features:

Features
======================

- Multipart Uploads and Downloads (high performance transfers)
- Support for AWS S3, Azure Storage, Google Cloud Storage
- Slicing of BAM and CRAM files by genomic region
- Client includes some samtools functionality such as viewing reads from a BAM
- MD5 validation of uploads and downloads
- ACL security using OAuth2 and scopes based on study codes
- Integrates with the SONG metadata system for data book keeping and consistency
- REST API with swagger docs


Projects Using Score
======================

1. **Cancer Collaboratory - Toronto**: https://storage.cancercollaboratory.org/swagger-ui.html
2. **AWS - Virginia**: https://virginia.cloud.icgc.org/swagger-ui.html


Getting Started
============================

Client

Server


License
=============
Copyright (c) 2018. Ontario Institute for Cancer Research

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.

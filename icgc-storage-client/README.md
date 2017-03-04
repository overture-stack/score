# ICGC Storage Client

Software for accessing ICGC data sets in affiliated cloud environments.

(Distribution module with formal ICGC naming / branding instead of DCC.)

## Usage

An example usage of the container which will download a remote file (with associated index file) having `object-id` `5b845b9a-3dcd-59ef-9f56-9a99396e988f` to `/tmp` on the docker host machine in "bundle" layout. The files will be written with ownership set to the current user (`/usr/bin/id -u`) and group (`/usr/bin/id -g`)

```shell
# Get latest image
pull icgc/icgc-storage-client

# Publish token
export ACCESSTOKEN=<access token from https://dcc.icgc.org>

# Make life easy for usage
alias icgc-storage-client="docker run -it --rm  -u $(id -u):$(id -g) -e ACCESSTOKEN -v /tmp:/data icgc/icgc-storage-client bin/icgc-storage-client"

# Usage with an example object-id from https://dcc.icgc.org
icgc-storage-client download --object-id 5b845b9a-3dcd-59ef-9f56-9a99396e988f --output-dir /data --output-layout bundle
```

## Azure

Profile: ``azure``

Experimental functionality 
The Storage Client running the ``azure`` profile works the same way as ``aws`` and ``collab`` with the following exceptions:

1. the number of parallel upload/download threads needs to be set very high (to offset the fact that blocks are so small). Settings like:

	```yaml
transport.memory = 8
transport.parallel = 24 # or 32 (assuming 250 MB blocks is defined on server) 
```
are required to get reasonable throughput.

2. Download operations work the same as before, and are resumable.

3. Currently, Azure is not able to store Blobs larger than 195 GB. There is an internal maximum size of 50,000-block blobs (each block is 4 MB) in Azure Storage.

4. Upload operations are not resumable and work differently than for other repositories. In addition to the 50,000-block limit above, there is also a 100,000 uncommitted-block limit  that could be reached depending on how many times an upload is retried without completing. If 100,000 blocks are uploaded without completing (i.e., a 50,000 block file is being uploaded, but is interrupted at the 25,000th block 4 times (each time, the upload restarts from the beginning), then the upload will fail and manual intervention will be required to clean things up in the Azure repository.

	Resumable uploads are technically feasible, but would require us to implement our own upload protocol from scratch. Currently, we are using the upload functionality supplied by Azure's Java SDK.

5. There is also a limit of 500 TB of data that can be stored in a single Azure storage account; If this is a real constraint, the Storage Service will need to be modified to handle this.

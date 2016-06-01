# ICGC Storage Client

Software for accessing ICGC data sets in affiliated cloud environments.

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

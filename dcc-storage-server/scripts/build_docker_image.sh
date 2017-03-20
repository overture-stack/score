#!/usr/bin/env bash
set -e
version=${1}

tar xf target/dcc-storage-server-${version}-dist.tar.gz
docker build -t quay.io/ucsc_cgl/redwood-storage-server:${version} dcc-storage-server-${version}
rm -r dcc-storage-server-${version}
echo "$(basename $0): built docker image: quay.io/ucsc_cgl/redwood-storage-server:${version}"

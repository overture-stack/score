#!/bin/bash
set -x
SCORE_CLIENT="/score-client/bin/score-client"

function upload() {
  $SCORE_CLIENT upload --manifest /data/$1 && echo "OK" || echo "FAILED"
}

function download() {
  DIR=/tmp/$1;test -d $DIR && rm -r $DIR;mkdir $DIR;$SCORE_CLIENT download --manifest /data/$1 --output-dir $DIR && echo "OK" || echo "FAILED"
}

echo "upload combined manifest"
upload combined.manifest

# Note: download manifests have 11 fields, upload manifests only have 3
# Write a test for downloading manifests at some point
#echo "download TEST-CA/manifest"
#download test-ca.manifest

#echo "download ABC123/manifest"
#download test-abc.manifest

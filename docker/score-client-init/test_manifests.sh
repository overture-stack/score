#!/bin/bash
set -x
SCORE_CLIENT="/score-client/bin/score-client"
token="f69b726d-d40f-4261-b105-1ec7e6bf04d5" # score.write, song.write

function upload() {
  $SCORE_CLIENT upload --manifest /data/$1 && echo "OK" || echo "FAILED"
}

function download() {
  DIR=/tmp/$1;test -d $DIR && rm -r $DIR;mkdir $DIR;$SCORE_CLIENT download --manifest /data/$1 --output-dir $DIR && echo "OK" || echo "FAILED"
}

/data/unpublish-analysis.sh $token TEST-CA a25ae8b4-097d-11ea-b4b9-41f0d4c18919
/data/unpublish-analysis.sh $token ABC123  a22f44d3-097d-11ea-b4b9-374b8c686482

echo "upload combined manifest"
upload combined.manifest

/data/publish-analysis.sh $token TEST-CA a25ae8b4-097d-11ea-b4b9-41f0d4c18919
/data/publish-analysis.sh $token ABC123  a22f44d3-097d-11ea-b4b9-374b8c686482

# Note: download manifests have 11 fields, upload manifests only have 3
# Write a test for downloading manifests at some point
echo "download TEST-CA/manifest"
download test-ca.manifest

echo "download ABC123/manifest"
download test-abc.manifest

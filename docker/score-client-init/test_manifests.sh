#!/bin/bash
function upload() {
  $SCORE_CLIENT upload --manifest /data/$1 && echo "OK" || echo "FAILED"
}

function download() {
  $SCORE_CLIENT download --manifest /data/$1 --output-dir /tmp && echo "OK" || echo "FAILED"
}


echo "upload combined manifest"
upload combined.manifest

echo "upload TEST-CA/manifest"
upload test-ca.manifest

echo "upload ABC123/manifest"
upload test-abc.manifest

echo "download TEST-CA/manifest"
download test-ca.manifest

echo "download ABC123/manifest"
download test-abc.manifest

echo "download combined manifest"
download combined.manifest

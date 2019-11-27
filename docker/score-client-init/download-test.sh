#!/bin/bash
export ACCESSTOKEN=$1
echo "=== Testing Downloads with token $ACCESSTOKEN"
echo
SCORE_CLIENT="/score-client/bin/score-client"
DIR="/tmp/download/$ACCESSTOKEN"
function download() {
  test -d $DIR && /bin/rm -r $DIR; mkdir $DIR;$SCORE_CLIENT download --output-dir $DIR --object-id $1 --verify-connection true && echo "OK" || echo "FAILED"
}

echo "download TEST-CA/controlled"
download 576f7386-e626-5d99-ae63-1b3ce9de308b
echo "download TEST-CA/open"
download 27612507-2070-5761-9dad-a0efeed5315a 

echo "download ABC123/controlled"
download d0522915-f5b6-584e-8409-c12fa928d089
echo "download ABC123/open"
download d4594aa2-4b32-5283-861c-bd5dd7ee6463

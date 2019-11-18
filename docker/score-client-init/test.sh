#!/bin/bash
set -x
export ACCESSTOKEN=$1
op=${2:-download}

SCORE_CLIENT="/score-client/bin/score-client"

function file() {
  $SCORE_CLIENT $op --file /data/$1 --md5 $2 --object-id $3 && echo "OK" || echo "FAILED"
}

function manifest() {
  $SCORE_CLIENT $op --manifest /data/$1 && echo "OK" || echo "FAILED"
}

echo "$op TEST-CA/controlled"
file TEST-CA/controlled.bam ac5d500ec64fa7dc287050c553b83f3a 576f7386-e626-5d99-ae63-1b3ce9de308b

echo "$op TEST-CA/open"
file TEST-CA/open.bam 0301ca53c86138baa75b7aa357055405 27612507-2070-5761-9dad-a0efeed5315a 

#echo "$op TEST-CA/manifest"
#manifest test-ca.manifest

echo "$op ABC123/controlled"
file ABC123/controlled.bam 91ed4a69ffe462ebb5dc4e6e78b31e38 d0522915-f5b6-584e-8409-c12fa928d089

echo "$op ABC123/open"
file ABC123/open.bam 92439c0cf911b91bfec4243af4284cc0 d4594aa2-4b32-5283-861c-bd5dd7ee6463
#echo "$op ABC123/manifest"
#manifest test-abc.manifest

#echo "$op combined manifest"
#manifest combined.manifest

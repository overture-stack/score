#!/bin/bash
token=${1:-f69b726d-d40f-4261-b105-1ec7e6bf04d5}
SCORE_CLIENT="/score-client/bin/score-client"
DIR="/tmp/download/$analysis_id"
test -d $DIR && rm -r $DIR;mkdir -p $DIR 
export ACCESSTOKEN=$token
set -x
$SCORE_CLIENT download --study-id $2 --analysis-id $3 --output-dir $DIR

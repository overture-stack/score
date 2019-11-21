#!/bin/bash
token=$1
studyId=$2
analysisId=$3
curl -XPUT --header "Authorization: Bearer $token" http://song-server:8080/studies/$studyId/analysis/unpublish/$analysisId
echo ""

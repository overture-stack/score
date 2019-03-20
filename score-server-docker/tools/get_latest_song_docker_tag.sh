#!/bin/bash

auth_token=$1
if [ "$auth_token" == "" ]; then
    curl --silent "https://api.github.com/repos/overture-stack/SONG/releases" \
    | jq '.[].tag_name | match("song-docker.*") | .string' \
    | head -1 | xargs echo
else
    curl --silent \
    -H "Authorization: Bearer $auth_token" \
    "https://api.github.com/repos/overture-stack/SONG/releases" \
    | jq '.[].tag_name | match("song-docker.*") | .string' \
    | head -1 | xargs echo
fi

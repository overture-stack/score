tag=`curl --silent  "https://api.github.com/repos/overture-stack/SONG/releases"  \
    | jq '.[].tag_name | match("^song-docker-\\\\d+\\\\.\\\\d+\\\\.\\\\d+$") | .string' \
    | head -1 \
    | xargs echo `

name="$tag.zip"
curl --silent "https://github.com/overture-stack/SONG/archive/$name"  -L -o $name

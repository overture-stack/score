#!/bin/bash
SONG="http://song-server:8080"
token="Bearer f69b726d-d40f-4261-b105-1ec7e6bf04d5" 
headers='-H "Content-Type: application/json" -H "Authorization: $token"'
echo "Checking isAlive"
curl $SONG/isAlive/ -i -H "Authorization: $token" 

#echo "Creating ABC"
#curl $SONG/studies/ABC123/ -i -H "Content-Type: application/json" -H "Authorization: $token" -d '{"name":"ABC123","studyId":"ABC123", "description":"woot","organization":"disorganized"}'  
#echo "Creating TEST-CA"
#"$SONG/studies/TEST-CA/" -i -H "Content-Type: application/json" -H "Authorization: $token" -d '{"name":"TEST-CA","studyId":"TEST-CA","description":"meh","organization":"sheer chaos"}'  
echo "Uploading ABC analysis"
curl $SONG/upload/ABC123 -i -H "Content-Type: application/json" -H "Authorization: $token" -d @/data/ABC123/analysis.json 

echo "Uploading TEST-CA analysis"
curl $SONG/upload/TEST-CA -i -H "Content-Type: application/json" -H "Authorization: $token" -d @/data/TEST-CA/analysis.json 
echo "Done"

#!/bin/bash
export PATH="/data:$PATH"
token1="" # empty 
token2="d5b12aed-b3c7-4075-87dc-d46a89e54d18" # score.TEST-CA.write,song.write
token3="07a5a12e-a85f-4248-a9a1-851a8062b6ac" # score.write, song.write

/data/unpublish-analysis.sh $token3 TEST-CA a25ae8b4-097d-11ea-b4b9-41f0d4c18919
/data/unpublish-analysis.sh $token3 ABC123  a22f44d3-097d-11ea-b4b9-374b8c686482

echo "************************************************************"
echo "Upload test with empty access token -- should all fail" 
echo "************************************************************"
upload-test.sh $token1 
echo ""
echo "************************************************************"
echo "Upload test with access token for project TEST-CA -- only last 2 should fail"
echo "************************************************************"
upload-test.sh $token2 
echo ""
echo "************************************************************"
echo "Upload test with access token with scope score.write -- first 2 should say already uploaded; second 2 should upload"
echo "************************************************************"
upload-test.sh $token3 


echo ""
/data/publish-analysis.sh $token3 TEST-CA a25ae8b4-097d-11ea-b4b9-41f0d4c18919
/data/publish-analysis.sh $token3 ABC123  a22f44d3-097d-11ea-b4b9-374b8c686482

echo "**************************************************************"
echo Downloading analysis by analysis id
echo "**************************************************************"
/data/download-analysis.sh $token3 TEST-CA a25ae8b4-097d-11ea-b4b9-41f0d4c18919

echo ""
echo "************************************************************"
echo "Download test with empty access token -- open access should succeed; closed should fail"
echo "************************************************************"
download-test.sh $token1
echo ""
echo "************************************************************"
echo "Download test with access token for project TEST-CA; open access and closed TEST-CA should succeed; closed ABC123 should fail"
echo "************************************************************"
download-test.sh $token2
echo ""
echo "************************************************************"
echo "Download test with score.write; everything should succeed"
echo "************************************************************"
download-test.sh $token3

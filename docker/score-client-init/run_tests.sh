#!/bin/bash
token1=""
token2=""
token3=""

./test.sh $token1 upload > score-write.log
./test.sh $token1 download >> score-write.log

./test.sh $token2 upload > test-ca.log
./test.sh $token2 download >> test-ca.log

./test.sh $token3 upload > no_token.log
./test.sh $token3 download >> no_token.log 

diff score-write.log score-write.expected
diff test-ca.log test-ca.expected
diff no_token.log no_token.expected

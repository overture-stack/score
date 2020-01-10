#!/bin/bash
#HERE=$(dirname $0)
#RUN_DIR=$HERE
#cd $HERE
RUN_DIR=$(pwd)
expected=$RUN_DIR/expected
result=$RUN_DIR/result
actual=$RUN_DIR/actual

eval . $RUN_DIR/bash_colors.sh

#set -x 
cd ../target

echo "Extracting client from tarball"
tar xf score-client-2.2.1-SNAPSHOT-dist.tar.gz

cd score-client-2.2.1-SNAPSHOT
echo "Setting up configuration files"

echo "metadata.url=https://example.song.org" >> ./conf/application.properties
echo "storage.url=https://example.score.org" >> ./conf/application.properties
echo "accessToken=myToken" >> ./conf/application.properties

export PATH=$PATH:`pwd`/bin

function validate {
  local test
  test=$1
  echo -n Test test${test}...
  want=${expected}${test}
  have=${result}${test}
  config=${actual}${test}
  grep -A 10 Configuration $have > $config
  diff -w $config $want && echo "${Green}OK${NC}"|| echo "${Red}FAILED${NC}" 
}

score-client info >& ${result}1
validate 1

STORAGE_URL=https://something.org score-client info >& ${result}2
validate 2

score-client --profile collab info >& ${result}3
validate 3

score-client --profile aws info >& ${result}4
validate 4

STORAGE_URL=https://something.org score-client --profile aws info >& ${result}5
validate 5

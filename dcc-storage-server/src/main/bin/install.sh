#!/bin/bash -e
#
# Copyright 2014(c) The Ontario Institute for Cancer Research. All rights reserved.

DCC_HOME=`dirname $0`/..; export DCC_HOME
SRV_HOME=/srv/dcc-bam-server

sudo cp ${DCC_HOME}/bin/dcc-bam /etc/init.d
sudo sed -i "s/\${SRV_HOME}/$(echo ${SRV_HOME} | sed -e 's/[\/&]/\\&/g')/g" /etc/init.d/dcc-bam
sudo sed -i "s/\${USER}/${USER}/g" /etc/init.d/dcc-bam
sudo chkconfig --add /etc/init.d/dcc-bam

echo "Mode (default/prod): "
read mode
echo "BAM Appliance AWS_ACCESS_KEY_ID: "
read accessId
echo "BAM Appliance AWS_SECRET_KEY: "
read secretKey

sudo sed -i "s/\${MODE}/$(echo $mode | sed -e 's/[\/&]/\\&/g')/g" ${DCC_HOME}/bin/dcc-bam-app
sudo sed -i "s/\${AWS_ACCESS_KEY_ID}/$(echo ${accessId} | sed -e 's/[\/&]/\\&/g')/g" ${DCC_HOME}/bin/dcc-bam-app
sudo sed -i "s/\${AWS_SECRET_ACCESS_KEY}/$(echo ${secretKey} | sed -e 's/[\/&]/\\&/g')/g" ${DCC_HOME}/bin/dcc-bam-app

sudo cp -r ${DCC_HOME} ${SRV_HOME}
sudo chown -R ${USER} ${SRV_HOME}
sudo /etc/init.d/dcc-bam
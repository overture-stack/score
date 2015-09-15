#!/bin/bash
#cd /home/ec2-user
export OBJECTSTORE_INSTANCE=objectstore.cancercollaboratory.org
export AWS_ACCESS_KEY_ID=
export AWS_SECRET_ACCESS_KEY=
export AWS_DEFAULT_REGION=us-east-1
/usr/bin/python /home/ec2-user/harvest.py


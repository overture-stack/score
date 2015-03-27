#!/bin/bash -e
#
# Copyright 2014(c) The Ontario Institute for Cancer Research. All rights reserved.


# disable ssh
chkconfig sshd off
service sshd stop
yum erase openssh-server

sed -e '/scripts-user/ s/^#*/#/' -i /etc/cloud/cloud.cfg.d/00_defaults.cfg
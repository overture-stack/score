#!/bin/bash -e
#
# Copyright 2014(c) The Ontario Institute for Cancer Research. All rights reserved.
#
# Description:
#   generate certificate for appliance and client
#
# Usage:
#   To generate certificates, ./gencert.sh
#
DCC_HOME=`dirname $0`; export DCC_HOME

keytool -genkeypair -alias appliance -keyalg RSA -dname "CN=repository.icgc.org,OU=Software Development,O=OICR,L=Ontario,S=Toronto,C=CA" -keypass password -keystore ${DCC_HOME}/appliance.jks -storepass password -validity 1095

keytool -exportcert -alias appliance -file ${DCC_HOME}/appliance-public.cer -keystore ${DCC_HOME}/appliance.jks -storepass password
keytool -importcert -keystore ${DCC_HOME}/client.jks -alias appliance -file ${DCC_HOME}/appliance-public.cer -storepass password -noprompt

mv ${DCC_HOME}/appliance.jks ${DCC_HOME}/../src/main/cert/
mv ${DCC_HOME}/client.jks ${DCC_HOME}/../../dcc-repository-client/src/main/resources/
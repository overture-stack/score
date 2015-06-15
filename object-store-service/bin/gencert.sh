#!/bin/bash -e
#
# Copyright 2015(c) The Ontario Institute for Cancer Research. All rights reserved.
#
# Description:
#   generate certificate for object store service and client
#
# Usage:
#   To generate certificates, ./gencert.sh <SERVICE-PASSWORD>
#
DCC_HOME=`dirname $0`; export DCC_HOME

keytool -genkeypair -alias object-store-service -keyalg RSA -dname "CN=icgc.cancercollaboratory.org,OU=Software Development,O=OICR,L=Ontario,S=Toronto,C=CA" -keypass $1 -keystore ${DCC_HOME}/service.jks -storepass $1 -validity 1095

keytool -exportcert -alias object-store-service -file ${DCC_HOME}/service-public.cer -keystore ${DCC_HOME}/service.jks -storepass $1
keytool -importcert -keystore ${DCC_HOME}/client.jks -alias object-store-service -file ${DCC_HOME}/service-public.cer -storepass CLIENT_SECRET -noprompt

mv ${DCC_HOME}/service.jks ${DCC_HOME}/../src/main/cert/
mv ${DCC_HOME}/client.jks ${DCC_HOME}/../../object-store-client/src/main/resources/
rm ${DCC_HOME}/service-public.cer
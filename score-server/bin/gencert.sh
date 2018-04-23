#!/bin/bash -e
#
# Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.
#
# This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
# You should have received a copy of the GNU General Public License along with
# this program. If not, see <http://www.gnu.org/licenses/>.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
# EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
# OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
# SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
# INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
# TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
# OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
# IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
# ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
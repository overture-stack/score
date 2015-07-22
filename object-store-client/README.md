Collaboratory - Object Store Client
===

Object Store Service for the Collaboratory. 

Build
---

First run [gencert.sh](../object-store-service/bin/gencert.sh):

`cd object-store-service`

`bin/gencert.sh <password>`

Next perform the build:

`cd object-store-client`

`mvn package`

Run
---

`bin/col-repo upload —manifest manifest.txt`


ICGC DCC - Storage Client
===

ICGC storage client for the ICGC storage system. 

Build
---

First run [gencert.sh](../dcc-storage-server/bin/gencert.sh):

```
cd dcc-storage-server
bin/gencert.sh <password>
```

Next perform the build:

```
cd object-store-client
mvn package
```

Run
---

```
bin/dcc-storage-client upload —manifest manifest.txt
```

Develop
---

The following will allow a developer to hit against production from the Eclipse IDE

### Entry Point

```
org.icgc.dcc.storage.client.ClientMain
```

### VM Arguments

```
-Dspring.config.location=src/main/conf/
-Dspring.profiles.active=prod
-Dclient.strictSsl=true
-Dclient.upload.serviceHostname=<host>
-DaccessToken=<token>
```

### Program Arguments

```
upload --manifest src/test/resources/fixtures/upload/manifest.txt
```



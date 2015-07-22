Collaboratory - Object Store Client
===

Object Store Client for the Collaboratory. 

Build
---

First run [gencert.sh](../object-store-service/bin/gencert.sh):

```
cd object-store-service
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
bin/col-repo upload —manifest manifest.txt
```

Develop
---

The following will allow a developer to hit against production from the Eclipse IDE

### Entry Point

```
collaboratory.storage.object.store.client.ClientMain
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
upload --manifest src/test/resources/fixtures/manifest.txt
```



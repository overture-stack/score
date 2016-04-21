# Installing the ICGC Storage System
An overview sentence goes here.

## Prerequisites
The storage system must be built using a Maven version between 3.0.3 and 3.2.5 (inclusive). [Maven Version Manager](http://mvnvm.org/) may prove useful.

## Download
The following JARs will be needed (available from the [ICGC Artifactory](https://seqwaremaven.oicr.on.ca/artifactory/dcc-release/org/icgc/dcc/)):
- [dcc-auth-server](https://seqwaremaven.oicr.on.ca/artifactory/dcc-release/org/icgc/dcc/dcc-auth-server/)
- [dcc-metadata-server](https://seqwaremaven.oicr.on.ca/artifactory/dcc-release/org/icgc/dcc/dcc-metadata-server/)
- [dcc-metadata-client]()
- List goes on...

## Build From Source
Clone and build the [dcc-storage](https://github.com/icgc-dcc/dcc-storage) project:
```
git clone git@github.com:icgc-dcc/dcc-storage.git
mvn
```

Once the project has been built, its artifacts can be used to run the storage system.

## Run the System
Start the auth-server:
```
foo bar baz
```

Start the metadata-server:
```
qux corge grault
```

Start the storage-server:
```
garply norf pugh
```

## Using the System
The `icgc-storage-client` subproject will have built an archive containing the script that should be used for all end-user interactions with the storage system. Look for `icgc-storage-client/target/icgc-storage-client-*-SNAPSHOT.tar.gz` and distribute this to anybody who will be using the storage system.

The end user can extract the archive then get started:
```
tar xvf icgc-storage-client-*-SNAPSHOT.tar.gz
cd icgc-storage-client-*-SNAPSHOT.tar.gz
bin/icgc-storage-client help
```

### Examples
Upload a file:
```
bin/icgc-storage-client upload --manifest manifest.txt --file data.bam
```
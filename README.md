# ICGC DCC - Storage

Storage and transfer system for ICGC cloud based projects against S3 and Azure backends.

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/201ae314ab3842baad25bc820069e90a)](https://www.codacy.com/app/icgc-dcc/dcc-storage?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=icgc-dcc/dcc-storage&amp;utm_campaign=Badge_Grade)

## Build

To compile, test and package the system, execute the following from the root of the repository:

```shell
mvn
```

## Run

See module-specific documentation below.

## Modules
Top level system modules:

- [Core](dcc-storage-core/README.md)
- [Client](dcc-storage-client/README.md)
- [File System](dcc-storage-fs/README.md)
- [Server](dcc-storage-server/README.md)
- [Test](dcc-storage-test/README.md)

Rebranding packaging:

- [ICGC Storage Client](icgc-storage-client/README.md) 

## Changes

Change log for the user-facing system modules may be found in [CHANGES.md](CHANGES.md).

## License

Copyright and license information may be found in [LICENSE.md](LICENSE.md).


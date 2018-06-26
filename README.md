# SCORe
Secure Cloud Object Repository

Formerly known as ICGC Storage and currently used as the storage and transfer system for ICGC cloud based projects against S3 and Azure backends.

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/201ae314ab3842baad25bc820069e90a)](https://www.codacy.com/app/overture-stack/score?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=overture-stack/score&amp;utm_campaign=Badge_Grade)
[![CircleCI](https://circleci.com/gh/overture-stack/score/tree/develop.svg?style=svg)](https://circleci.com/gh/overture-stack/score/tree/develop)
[![Slack](http://slack.overture.bio/badge.svg)](http://slack.overture.bio)

## Build

To compile, test and package the system, execute the following from the root of the repository:

```shell
mvn
```

## Run

See module-specific documentation below.

## Modules
Top level system modules:

- [Core](score-core/README.md)
- [Client](score-client/README.md)
- [File System](score-fs/README.md)
- [Server](score-server/README.md)
- [Test](score-test/README.md)

## Changes

Change log for the user-facing system modules may be found in [CHANGES.md](CHANGES.md).

## License

Copyright and license information may be found in [LICENSE.md](LICENSE.md).


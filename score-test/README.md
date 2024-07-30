# SCORe - Storage Test

Module used for integration testing.

## Libraries

The test relies on the following components:

- [S3 Ninja](http://s3ninja.net/)
- [Embedded MongoDB](http://flapdoodle-oss.github.io/de.flapdoodle.embed.mongo/)

## Build

To compile, test and package the module, execute the following from the root of the repository:

```shell
mvn -am -pl score/score-test
```
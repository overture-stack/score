<h1 align="center">SCORe</h1>

<p align="center">Secure Cloud Object Repository</p>

<p align="center">Formerly known as ICGC Storage and currently used as the storage and transfer system for ICGC cloud based projects against S3 and Azure backends.</p>

<p align="center"><a href="http://www.overture.bio/products/score" target="_blank"><img alt="General Availability" title="General Availability" src="http://www.overture.bio/img/progress-horizontal-GA.svg" width="320" /></a></p>

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/201ae314ab3842baad25bc820069e90a)](https://www.codacy.com/app/overture-stack/score?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=overture-stack/score&amp;utm_campaign=Badge_Grade)
[![Documentation Status](https://readthedocs.org/projects/score-docs/badge/?version=latest)](https://score-docs.readthedocs.io/en/latest/?badge=latest)
[![Slack](http://slack.overture.bio/badge.svg)](http://slack.overture.bio)

## Documentation

Explore documentation with the Score [Read the Docs](https://score-docs.readthedocs.io/en/develop/introduction.html).

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

## Developement
Several `make` targets are provided for locally deploying dependent services using docker. 
By using this, the developer will be able to replicate a live environment for score-server and score-client. 
It allows the user to develop locally, and test uploads/downloads in an isolated environment.

There are 2 modes:

### 1. Developement Mode
The purpose of this mode is to decrease the wait time between building and testing against dependent services.
This mode will run a `mvn package` if the `*-dist.tar.gz` files are missing, and copy them into a container for them to be run. 
This method allows for fast developement, since the `mvn package` step is handled on the **Docker host**.
In addition, the debug ports `5005` and `5006` are exposed for both `score-client` and `score-server`, respectively, allowing developers to debug the docker containers.
This mode can be enabled using the `DEMO_MODE=0` override. This is the default behaviour if the variable `DEMO_MODE` is not defined.

#### Debugging the score-client with IntelliJ
Since the JVM debug port is exposed by the `score-client` docker container, IntelliJ can **remotely debug** a running docker container. 
To do this, a **docker image run profile** must be created with the configuration outputted by the `make intellij-score-client-config` command, which will output a basic upload command, however it can be modified to be any score-client command.
Then, a **remote debug profile** must be created, with the following config:

```
Host: localhost
Port: 5005
Use module classpath: score-client
```
and in the `Before launch: Activate tool window` section, click the `+` sign, and select `Launch docker before debug`. 
Then ensure the `Docker configuration` field is set to the name of the previously created **docker image run profile**  and that `Custom Options` is set to `-p 5005:5005`. In order for the debugger to bind to the debug port in time, 
a delay needs to be introduced after starting the container. To do this, click the `+` sign again, and select `Launch docker before debug`, and select `Run External Tool` and a window will pop-up. Input the following:

```
Name:      Sleep for 5 seconds
Program:   /usr/bin/sleep
Arguments: 5
```
and click `OK`.

Finally,  start debugging by simply running the **remote debug profile** and it will call the **docker image run profile** before launch. 

#### Debugging the score-server with IntelliJ
Since the `score-server` is a server and exposes the 5006 debug port, configuration is much easier. First, start the server with `make clean start-score-server`. Then, create a **remote debug profile** in Intellij with the following configuration:
```
Host: localhost
Port: 5006
Use module classpath: score-server
```
and then run it in debug mode. 



### 2. Demo Mode
The purpose of this mode is to demo the current `score-server` and `score-client` code by building it in **inside the Docker image**, 
as opposed to the **Docker host** as is done in Developement mode and then running the containers.
This mode will not run `mvn package` on the Docker host, but instead inside the Docker container.
This method is very slow, since maven will download dependencies every time a build is triggered, however creates a completely isolated environment for testing.
This mode can be enabled using the `DEMO_MODE=1` make variable override. For example, to start the score-server, the following command would be run:

```bash
make start-score-server DEMO_MODE=1
```

For more information on the different targets, run `make help` or read the comments above each target for a description


## Changes

Change log for the user-facing system modules may be found in [CHANGES.md](CHANGES.md).

## License

Copyright and license information may be found in [LICENSE.md](LICENSE.md).

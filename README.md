ICGC DCC - Repository
===

Parent project of the [ICGC DCC Repository](https://wiki.oicr.on.ca/display/DCCSOFT/DCC+Repository+Specification) system.

Setup
---

Clone the repository:

`git clone https://github.com/icgc-dcc/dcc-repository.git`

Install Maven 3.2.1:

[http://maven.apache.org/download.cgi](http://maven.apache.org/download.cgi)

Build
---

To build, test and install _all_ modules in the system:

`mvn`


To build, test and install _only_ the Core sub-system module:

`mvn -amd -pl dcc-repository-core`
	
To build, test and install _only_ the Client sub-system module:

`mvn -amd -pl dcc-repository-client`

To build, test and install _only_ the Server sub-system module:

`mvn -amd -pl dcc-repository-server`

To build, test and install _only_ the Proxy sub-system module:

`mvn -amd -pl dcc-repository-proxy`


Run
---

See specific module documentation below.

Modules
---
Top level system modules:

- [Core](dcc-repository-core/README.md)
- [Client](dcc-repository-client/README.md)
- [Server](dcc-repository-server/README.md)
- [Proxy](dcc-repository-proxy/README.md)


# Score

Score is a file transfer service designed for cloud-based projects, providing a robust API for secure file transfer and storage operations. It serves as an intermediary between object storage systems and user authorization mechanisms, using pre-signed URLs for efficient and protected data access.

</br>

> 
> <div>
> <img align="left" src="ov-logo.png" height="50"/>
> </div>
> 
> *Score is part of [Overture](https://www.overture.bio/), a collection of open-source software microservices used to create platforms for researchers to organize and share genomics data.*
> 
> 
## Repository Structure
The repository is organized with the following directory structure:
```
.
├── /score-client
├── /score-core
├── /score-fs
├── /score-server
└── /score-test
```

- **Score-client:** Command line app for uploading and downloading files
- **Score-core:** Core library containing shared utilities and data models
- **Score-fs:** File system operations module for managing local files
- **Score-server:** Main server application that handles object storage and transfers
- **Score-test:** Integration and end-to-end test suite components

## Documentation

Technical resources for those working with or contributing to the project are available from our official documentation site, the following content can also be read and updated within the `/docs` folder of this repository.

- **[Score Overview](https://main--overturedev.netlify.app/docs/core-software/Score/overview)** 
- [**Setting up the Development Enviornment**](https://main--overturedev.netlify.app/docs/core-software/Score/setup)
- [**Common Usage Docs**](https://main--overturedev.netlify.app/docs/core-software/Score/setup)

##  Development Environment

- [Java 11 (OpenJDK)](https://openjdk.java.net/projects/jdk/11/)
- [Maven 3.5+](https://maven.apache.org/) (or use provided wrapper)
- [VS Code](https://code.visualstudio.com/) or preferred Java IDE
- [Docker](https://www.docker.com/) Container platform

## Support & Contributions

- For support, feature requests, and bug reports, please see our [Support Guide](https://main--overturedev.netlify.app/community/support).
- For detailed information on how to contribute to this project, please see our [Contributing Guide](https://main--overturedev.netlify.app/docs/contribution).

## Related Software 

The Overture Platform includes the following Overture Components:

</br>

|Software|Description|
|---|---|
|[Score](https://github.com/overture-stack/score/)| Transfer data to and from any cloud-based storage system |
|[Song](https://github.com/overture-stack/song/)| Catalog and manage metadata associated to file data spread across cloud storage systems |
|[Maestro](https://github.com/overture-stack/maestro/)| Organizing your distributed data into a centralized Elasticsearch index |
|[Arranger](https://github.com/overture-stack/arranger/)| A search API with reusable search UI components |
|[Stage](https://github.com/overture-stack/stage)| A React-based front-data portal UI |
|[Lyric](https://github.com/overture-stack/lyric)| A data-agnostic tabular data submission system |
|[Lectern](https://github.com/overture-stack/lectern)| A simple web browser UI that integrates Ego and Arranger |

If you'd like to get started using our platform [check out our quickstart guides](https://main--overturedev.netlify.app/guides/getting-started)

## Funding Acknowledgement

Overture is supported by grant #U24CA253529 from the National Cancer Institute at the US National Institutes of Health, and additional funding from Genome Canada, the Canada Foundation for Innovation, the Canadian Institutes of Health Research, Canarie, and the Ontario Institute for Cancer Research.

# Downloading Data

# Using the Swagger API

## Using the Score Client

The Score command-line client is a tool designed to enable user interactions with Score endpoints.

### Installing the Score-Client

To run the score-client using a Docker image, you need to provide specific environment variables including the Score server `STORAGE_URL`, the Song server `METADATA_URL`, and a valid `ACCESSTOKEN`.

Use the following templated command to run a Score Client docker container:

```bash
docker run -d --name score-client \
    -e ACCESSTOKEN=${token} \
    -e STORAGE_URL=${scoreServerUrl} \
    -e METADATA_URL=${songServerUrl} \
    --network="host" \
    --platform="linux/amd64" \
    --mount type=bind,source="$(pwd)",target=/output \
    ghcr.io/overture-stack/score:latest
```

Replace all placeholders `${}` with the appropriate values for your environment.

<details>

  <summary><b>Click here for a detailed breakdown</b></summary>

<br></br>

  - `-d` runs the container in detached mode, meaning it runs in the background and does not receive input or display output in the terminal.


  - `-e ACCESSTOKEN=${token}` will be the access token supplied by the platform's authorization and authentication service. For Overture, this will be Ego or Keycloak. API keys can be generated by users by logging into the front-end stage UI and navigating to their profile page.


  - `-e STORAGE_URL=${scoreServerUrl}` is the URL for the Score server that the Score-Client will interact with.


  - `-e METADATA_URL=${songServerUrl}` is the URL for the Song server that the Score-Client will interact with.


  - `--network="host"` uses the host network stack inside the container, bypassing the usual network isolation. This means the container shares the network namespace with the host machine.


  - `--platform="linux/amd64"` specifies the platform the container should emulate. In this case, it's set to linux/amd64, indicating the container is intended to run on a Linux system with an AMD64 architecture.


  - `--mount type=bind,source="$(pwd)",target=/output` mounts a directory of choice and its contents (volume) from the host machine to the container. Any changes made to the files in this directory will be reflected in both locations.

---

</details>
<br></br>


**Note:** For step-by-step instructions on installing and using the Song and Score clients, including downloading and uploading data, see our platform guides on [CLI submissions](https://www.overture.bio/documentation/guides/submission/clientsubmission/) and [CLI downloads](https://www.overture.bio/documentation/guides/download/clientdownload/).

### Downloading with the Score Client

File downloads can be run using the Score Client's `download` command.

The `download` command offers various methods for downloading file data. The main methods are as follows:

- `--analysis-id`: Downloads files for a specific <a href="/documentation/song" target="_blank" rel="noopener noreferrer">Song</a> analysis ID. 
- `--manifest`: Downloads specific files based on a manifest file ID, manifest file URL, or path to the manifest file.
- `--object-id`: Downloads a specific file object ID.
- `--program-id`: Downloads files for a specific <a href="/documentation/song" target="_blank" rel="noopener noreferrer">Song</a> program ID.
- `--study-id`: Downloads files for a specific <a href="/documentation/song" target="_blank" rel="noopener noreferrer">Song</a> study ID.

The table below details the options available when using the Score-Client `download` command:

| Option | Description |
| -------| ------------|
| `--analysis-id` | Download files for a specific <a href="/documentation/song" target="_blank" rel="noopener noreferrer">Song</a> analysis ID. |
| `--force` | Re-download the file if it already exists locally (overrides local file). |
| `--index` | If available, also download the file index. |
| `--length` | Restrict the download size to this number of bytes. By default, the whole file is downloaded unless this option is specified. |
| `--manifest` | Download specific files based on a manifest file ID, URL, or its path. |
| `--object-id` | Download a specific file object ID. |
| `--offset` | Byte position in the source file from where the download begins. By default, the whole file is downloaded unless this option is specified. |
| `--output-dir` | Path to the output directory where files will be downloaded to. |
| `--output-layout` | Layout of the output directory, one of: |
| | * `bundle` : Saved according to the filename under the Song bundle ID directory. |
| | * `filename` : Saved according to the filename in the output directory. |
| | * `id` : Saved according to the object ID in the output directory. |
| `--program-id` | Download files for a specific <a href="/documentation/song" target="_blank" rel="noopener noreferrer">Song</a> program ID. |
| `--study-id` | Download files for a specific <a href="/documentation/song" target="_blank" rel="noopener noreferrer">Song</a> study ID. |
| `--validate` | If available, validate the file using the MD5 checksum. |
| `--verify-connection` | First verify the connection to the object storage repository. |

### Download Example

Here is an example of downloading files using a previously generated manifest file from Song.

Execute the following command from your home directory:

```shell
docker exec score-client sh -c "score-client download --manifest ./<manifestDirectory>/manifest.txt --output-dir ./<outputDirectory>"
```

-  `<manifestDirectory>` represents the location of the earlier generated manifest file
- `<outputDirectory>` specifies where you intend to download the files

**What is a Manifest?** To understand more about key terms in Overture's data workflows, check this guide on [data submission using Song and Score](../../Song/03-Usage/01-submitting-data.md).

If successful the Score Client will indicate the download has completed.
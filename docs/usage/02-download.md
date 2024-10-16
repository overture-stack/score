# File Download

Data download using the Score Client.

:::info Download Guide
For detailed step-by-step instructions on using Song and Score clients for data downloads, see our [**platform guide on CLI downloads**](/guides/user-guides/cli-downloads).
:::

## Installing Score-Client

To run the score-client using Docker, provide the following environment variables:
- `STORAGE_URL`: Score server URL
- `METADATA_URL`: Song server URL
- `ACCESSTOKEN`: Valid access token

Use this command to run a Score Client docker container:

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

Replace all `${}` placeholders with your environment's values.

<details>
  <summary><b>Detailed command breakdown</b></summary>

  - `-d`: Runs container in detached mode (background)
  - `-e ACCESSTOKEN=${token}`: Access token from the platform's auth service
  - `-e STORAGE_URL=${scoreServerUrl}`: Score server URL
  - `-e METADATA_URL=${songServerUrl}`: Song server URL
  - `--network="host"`: Uses host network stack
  - `--platform="linux/amd64"`: Specifies container platform
  - `--mount type=bind,source="$(pwd)",target=/output`: Mounts current directory to container's `/output`

</details>

## Downloading File Data

Use the Score Client's `download` command to retrieve file data using any the following download methods:

- `--analysis-id`: Download files for a specific Song analysis ID
- `--manifest`: Download files based on a manifest file
- `--object-id`: Download a specific file object
- `--program-id`: Download files for a specific Song program
- `--study-id`: Download files for a specific Song study

### Download Options

| Option | Description |
|--------|-------------|
| `--force` | Re-download existing files (overwrite) |
| `--index` | Download file index if available |
| `--length` | Limit download size (bytes) |
| `--offset` | Start download from specific byte position |
| `--output-dir` | Specify download directory |
| `--output-layout` | Set output directory layout (`bundle`, `filename`, or `id`) |
| `--validate` | Validate file using MD5 checksum |
| `--verify-connection` | Verify object storage connection before download |

### Download Example

To download using a manifest file:

```shell
docker exec score-client sh -c "score-client download --manifest ./<manifestDirectory>/manifest.txt --output-dir ./<outputDirectory>"
```

- Replace `<manifestDirectory>` with the manifest file location
- Replace `<outputDirectory>` with your desired download location

:::info Score Client Reference Doc
For more information see our [**Score Client command reference documentation**](/docs/core-software/Score/usage/client-reference)
:::
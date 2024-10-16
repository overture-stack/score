# File Upload

Data uploads using the Client or API.

:::info Data Submission Guide
For detailed step-by-step instructions on using Song and Score clients for data submissions, see our [**platform guide on CLI submissions**](/guides/user-guides/cli-submissions).
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

## Uploading File Data

Use the Score Client's `upload` command to upload file data. Main upload methods:

- `--file`: Upload a specific file by path
- `--manifest`: Upload files based on a manifest file
- `--object-id`: Upload a specific file by object ID

### Upload Options

| Option | Description |
|--------|-------------|
| `--force` | Re-upload existing files (overwrite) |
| `--md5` | Specify MD5 checksum of the file |
| `--validate` | Validate file using MD5 checksum |
| `--verify-connection` | Verify object storage connection before upload |

### Upload Example

To upload using a manifest file:

```bash
docker exec score-client sh -c "score-client upload --manifest ./<directory>/manifest.txt"
```

Replace `<directory>` with the location of your manifest file.

A successful upload will produce output similar to:

```shell
Uploading object: '/home/ubuntu/songdata/input-files/example.vcf.gz.idx' using the object id e98daf88-fdf8-5a89-9803-9ebafb41de94
100% [##################################################]  Parts: 1/1, Checksum: 100%, Write/sec: 1000B/s, Read/sec: 0B/s
Finalizing...
Total execution time:         3.141 s
Total bytes read    :               0
Total bytes written :              24
Upload completed
Uploading object: '/home/ubuntu/songdata/input-files/example.vcf.gz' using the object id 440f4559-e905-55ec-bdeb-9518f823e287
100% [##################################################]  Parts: 1/1, Checksum: 100%, Write/sec: 7.8K/s, Read/sec: 0B/s
Finalizing...
Total execution time:         3.105 s
Total bytes read    :               0
Total bytes written :              52
Upload completed
```

:::info Support
If you encounter any issues or have questions, please don't hesitate to reach out through our relevant [community support channels](/community/support)
:::
# Client Reference

Commands and options supported by the Score client.

## Download

- The `download` command retrieves file object(s) from the remote storage repository.

- **Usage:** `score-client download [OPTIONS]`

    | Option | Description |
    |--------|-------------|
    | `--analysis-id` | Download files for a specific Song analysis ID |
    | `--force` | Force re-download of existing file (overrides local file) |
    | `--index` | Also download file index if available |
    | `--length` | Limit number of bytes to download (default: entire file) |
    | `--manifest` | Download files based on manifest file ID, URL, or path |
    | `--object-id` | Download a specific file object ID |
    | `--offset` | Starting byte position for download (default: 0) |
    | `--output-dir` | Path to download directory |
    | `--output-layout` | Layout of output directory (bundle, filename, or id) |
    | `--program-id` | Download files for a specific Song program ID |
    | `--study-id` | Download files for a specific Song study ID |
    | `--validate` | Perform MD5 checksum validation if available |
    | `--verify-connection` | Verify connection to object storage repository first |

## Help

- The `help` command displays detailed information about Score client commands and options.

## Info

- The `info` command shows active configuration details of the Score client.

- **Usage:** `score-client info [OPTIONS]`

    | Option | Description |
    |--------|-------------|
    | `--verbose` | Display all Score client configuration properties |

## Manifest

- The `manifest` command shows entries of a specific Score manifest file.

- **Usage:** `score-client manifest [OPTIONS]`

    | Option | Description |
    |--------|-------------|
    | `--manifest` | Manifest file ID, URL, or path to display |

## Mount

- The `mount` command provides a read-only FUSE filesystem view of the object storage repository.

- **Usage:** `score-client mount [OPTIONS]`

    | Option | Description |
    |--------|-------------|
    | `--cache-metadata` | Cache metadata on local disk for faster load times |
    | `--daemonize` | Run mount point in background |
    | `--layout` | Mount point directory layout (bundle or object-id) |
    | `--manifest` | Manifest file to mount contents for |
    | `--mount-point` | FUSE file system mount point |
    | `--options` | Additional file system mount options |
    | `--verify-connection` | Verify connection to object storage repository first |

## Upload

- The `upload` command sends file object(s) to the remote storage repository.

- **Usage:** `score-client upload [OPTIONS]`

    | Option | Description |
    |--------|-------------|
    | `--file` | Path to specific file for upload |
    | `--force` | Force re-upload of existing file (overrides repository file) |
    | `--manifest` | Upload files using manifest file ID, URL, or path |
    | `--md5` | MD5 checksum of file to upload |
    | `--object-id` | Upload specific file by object ID |
    | `--validate` | Perform MD5 checksum validation if available |
    | `--verify-connection` | Verify connection to object storage repository first |

## Url

- The `url` command reveals the URL of a specific file object in the storage repository.

- **Usage:** `score-client url [OPTIONS]`

    | Option | Description |
    |--------|-------------|
    | `--object-id` | Object ID of file to display URL for |

## Version

- The `version` command provides current version details of the Score client.

## View

- The `view` command stores locally and showcases a SAM or BAM file.

- **Usage:** `score-client view [OPTIONS]`

    | Option | Description |
    |--------|-------------|
    | `--bed-query` | BED format file with ranges to query (overrides --query) |
    | `--contained` | Output alignments completely contained in specified region |
    | `--header-only` | Output only the SAM/BAM file header |
    | `--input-file` | Local path to BAM file for querying |
    | `--input-file-index` | Local path to index file (requires --input-file) |
    | `--manifest` | Manifest file for querying object IDs and ranges |
    | `--object-id` | Specific object ID to download slice from |
    | `--output-file` | Name of output file |
    | `--output-format` | Output file format (SAM or BAM) |
    | `--output-dir` | Path to output directory (with --manifest) |
    | `--output-index` | Write index files to output (with --manifest) |
    | `--output-original-header` | Output entire original header |
    | `--output-type` | Structure of output file (CROSS, MERGED, or TRIMMED) |
    | `--query` | Query for BAM file content extraction (coordinate format) |
    | `--reference-file` | Local path to FASTA file for CRAM decoding |
    | `--stdout` | Send output to stdout (forces SAM format) |
    | `--verify-connection` | Verify connection to object storage repository first |

## Additional Options

Additional option flags available for the `score-client` executable:

| Option | Description |
|--------|-------------|
| `--profile` | Define specific environment profile for configuration |
| `--quiet` | Run client in quiet mode with minimal info messages |
| `--silent` | Run client in silent mode without any info messages |

## Need Help?

If you encounter any issues or have questions, please don't hesitate to reach out through our relevant [community support channels](/community/support)
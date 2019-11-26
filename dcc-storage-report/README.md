# ICGC DCC - Storage Audit Reports

Standalone program to parse our own Audit log files and compile basic statistics. Note that DCC Storage audit logs track REQUESTS received for uploads/downloads - 
whether the operation completed successfully or not, is not available.  

The program fetches two sets of reference data (Object types and User e-mail addresses) from their systems of record

This is checked-in as a Maven Module but is not included in the Reactor so it will not be built with the rest of the DCC Storage project. It can be built independently using Maven


## Build

To compile and package the system, execute the following from the module directory:

```shell
mvn clean package
```

## Run

Can be run one-off using the java -jar command, or scheduled as a cron job using a shell script similar to (from https://virginia.cloud.icgc.org):

```
#!/bin/bash -e

export HOME="/home/ec2-user"

_now=$(date +"%m_%d_%Y-%H:%M:%S")

echo "Starting:" $_now

cd /icgc/audit/logs
aws s3 sync s3://oicr.icgc.audit/virginia.cloud.icgc.org/ . --exclude "*" --include "dcc-storage.audit.*"
cd /icgc/audit
java -jar dcc-storage-auditreports-0.1-SNAPSHOT.jar -d /icgc/audit/logs -r BY_USER BY_DATE BY_OBJECT BY_OBJECT_BY_USER -o /icgc/audit/reports

_now=$(date +"%m_%d_%Y-%H:%M:%S")
echo "Ending:" $_now
```

The report generator looks for audit log files in the specified, locally-accessible directory only. This script file will use the AWS CLI to SYNC 
the s3://oicr.icgc.audit bucket to ensure the most recent log files are available.

This script assumes that the AWS credentials are located in the /home/ec2-user/.aws directory - which is why $HOME is explicitly set in the script. 
See http://serverfault.com/questions/614890/cant-run-aws-cli-from-cron-credentials 

## Options
```
    -c, --console  
      Show H2 db console in browser to verify load  
      Default: false  
    -j, --digests  
      Path of JSON file containing MD5-hashed access tokens to use instead of querying auth server directly  
    -h, -?, --help  
      Show this documentation  
      Default: false  
  * -d, --logdir  
      Path containing audit log files named *.audit.[YYYY-MM-DD].log  
      Default: /tmp  
    -f, --logfile  
      Name of a single log file to process  
    -o, --output-dir  
      Name of output directory  
      Default: /tmp  
    -r, --report  
      type of report(s) to generate: BY_DATE, BY_USER, BY_OBJECT, BY_OBJECT_BY_USER. Multiple options can be specified (separated by spaces)  
      Default: [BY_USER]  
```
       
## License

Copyright and license information may be found in [LICENSE.md](LICENSE.md).


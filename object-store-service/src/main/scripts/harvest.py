import boto3
import os
import fnmatch
import shutil

source = '/icgc/object-store-service-0.0.17/logs'

def all_files(root, patterns='*', single_level=False, yield_folders=False):
    # Expand patterns from semicolon-separated string to list
    patterns = patterns.split(';')
    for path, subdirs, files in os.walk(root):
        if yield_folders:
            files.extend(subdirs)
        files.sort( )
        for name in files:
            for pattern in patterns:
                if fnmatch.fnmatch(name, pattern):
                    yield os.path.join(path, name)
                    break
        if single_level:
            break

s3 = boto3.resource('s3')

instance = os.getenv('OBJECTSTORE_INSTANCE', 'default')
print 'Instance: ' + instance

for p in all_files(source, 'objectstore.*.log', True):
    fname = os.path.basename(p)
    # print p
    # print fname 

    # Upload a new file
    data = open(p, 'rb')
    keyval = instance + '/' + fname 
    s3.Bucket('oicr.icgc.audit').put_object(Key=keyval, Body=data)

    # to be replaced with operation to just delete audit logs instead of keeping them
    backup = source + '/archive/' + fname
    shutil.move(p, backup)

    # os.remove(p)

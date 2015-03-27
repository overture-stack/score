# generate certificate and private key
mkdir /tmp/cert
cd /tmp/cert
openssl genrsa -out privatekey.pem 1024
openssl req -new -x509 -key privatekey.pem -out publickey.cer -days 365
openssl x509 -in publickey.cer -out certificate.pem

# 
sudo -E su
$EC2_AMITOOL_HOME/bin/ec2-bundle-vol -i /srv/dcc-bam-server/cert/appliance.jks -k /tmp/cert/privatekey.pem -c /tmp/cert/certificate.pem -u 794321122735 -r x86_64 -e /tmp/cert
exit

ec2-upload-bundle -b <my-s3-bucket/bundle_folder/bundle_name> -m /tmp/image.manifest.xml -a <your_access_key_id> -s <your_secret_access_key> --region us-east-1
ec2-register oicr.dcc.bam.appliance/0/image.manifest.xml -n secured_dcc_bam_appliance_s3 -O AKIAJHA5HHADSL7K4B5Q -W iSw2TOOFQVqFyZTV4e4Bg6/1bsCyb56UTL/sZ1b3  --region us-east-1 -a x86_64 --virtualization-type hvm
#!/bin/bash
AWS_REGION=eu-west-2
BACK_UP_BUCKET="certificate-backup"
TEST_CLIENT_SECRET="testSecret"
TEST_CLIENT_SECRET_VALUE="{\"eventType\":[\"default\"]}"

#Create certficate backup bucket
awslocal s3 mb s3://$BACK_UP_BUCKET
#Copy client certificate to bucket
awslocal s3 cp "./client.p12" "s3://certificate-backup/testclient/client.p12"
awslocal secretsmanager create-secret --name $TEST_CLIENT_SECRET --description "Test Client Filter" --secret-string $TEST_CLIENT_SECRET_VALUE


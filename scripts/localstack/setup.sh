#!/bin/bash
AWS_REGION="eu-west-2"
AWS_ACCESS_KEY_ID="test"
AWS_SECRET_ACCESS_KEY="test"
BACK_UP_BUCKET="certificate-backup"
TEST_CLIENT_SECRET="testSecret"
TEST_CLIENT_SECRET_VALUE="{\"eventType\":[\"default\"]}"

aws configure set region $AWS_REGION
aws configure set aws_access_key_id $AWS_ACCESS_KEY_ID
aws configure set aws_secret_access_key $AWS_SECRET_ACCESS_KEY
#Create certficate backup bucket
aws --endpoint-url=http://localhost:4566 s3 mb s3://$BACK_UP_BUCKET
#Copy client certificate to bucket
aws --endpoint-url=http://localhost:4566 s3 cp "./scripts/localstack/client.p12" "s3://certificate-backup/testclient/client.p12"
aws --endpoint-url=http://localhost:4566 secretsmanager create-secret --name $TEST_CLIENT_SECRET --description "Test Client Filter" --secret-string $TEST_CLIENT_SECRET_VALUE

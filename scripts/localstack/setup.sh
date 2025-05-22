#!/bin/bash
AWS_REGION="eu-west-2"
BACK_UP_BUCKET="certificate-backup"
TEST_CLIENT_SECRET_NAME="testSecret"
TEST_CLIENT_SECRET_VALUE="{\"eventType\":[\"default\"]}"

echo "Checking environment variables are set"
echo "AWS_ACCESS_KEY_ID: ${AWS_ACCESS_KEY_ID:?AWS_ACCESS_KEY_ID must be set}"
echo "AWS_SECRET_ACCESS_KEY: ${AWS_SECRET_ACCESS_KEY:?AWS_SECRET_ACCESS_KEY must be set}"

echo "Configuring AWS CLI"
aws configure set region $AWS_REGION
aws configure set aws_access_key_id $AWS_ACCESS_KEY_ID
aws configure set aws_secret_access_key $AWS_SECRET_ACCESS_KEY

echo "Creating certificate backup bucket"
aws --endpoint-url=http://localhost:4566 s3 mb s3://$BACK_UP_BUCKET

echo "Copying client certificate to bucket"
aws --endpoint-url=http://localhost:4566 s3 cp "./scripts/localstack/client.p12" "s3://certificate-backup/testclient/client.p12"
aws --endpoint-url=http://localhost:4566 secretsmanager create-secret --region $AWS_REGION --name $TEST_CLIENT_SECRET_NAME --description "Test Client Filter" --secret-string $TEST_CLIENT_SECRET_VALUE

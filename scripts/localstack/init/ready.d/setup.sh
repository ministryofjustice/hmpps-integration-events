#!/bin/bash
AWS_REGION="eu-west-2"
BACK_UP_BUCKET="certificate-backup"
TOPIC_FILTER_KEY="testSecret"
TOPIC_FILTER_VALUE="{\"eventType\":[\"default\"]}"

# Create certificate backup bucket
awslocal s3 mb s3://$BACK_UP_BUCKET

# Copy client certificate to bucket
awslocal s3 cp "/etc/localstack/client.p12" "s3://certificate-backup/testclient/client.p12"
awslocal secretsmanager create-secret --region $AWS_REGION --name $TOPIC_FILTER_KEY --description "Test Client Filter" --secret-string $TOPIC_FILTER_VALUE
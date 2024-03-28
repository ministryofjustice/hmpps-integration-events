#!/bin/bash

echo "Creating dummy AWS profile..."
mkdir -p ~/.aws
cat > ~/.aws/credentials <<EOF
[dummy]
aws_access_key_id = dummy
aws_secret_access_key = dummy
EOF
echo "Created dummy AWS profile"

export AWS_PROFILE=dummy

echo "Creating SNS topic..."
sns_topic_arn=$(aws --endpoint-url=http://localhost:4566 sns create-topic --name probation-case-registration-added --output text)
echo "SNS topic created: $sns_topic_arn"

echo "Creating SQS queue..."
sqs_queue_url=$(aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name probation-case-registration-added --output text)
echo "SQS queue created: $sqs_queue_url"

echo "Fetching AWS account ID..."
aws_account_id=$(aws --endpoint-url=http://localhost:4566 sts get-caller-identity --query Account --output text)
echo "AWS account ID: $aws_account_id"

echo "Constructing SQS endpoint ARN..."
sqs_endpoint_arn="arn:aws:sqs:eu-west-2:${aws_account_id}:probation-case-registration-added"
echo "SQS endpoint ARN: $sqs_endpoint_arn"

echo "Subscribing SQS queue to SNS topic..."
aws --endpoint-url=http://localhost:4566 sns subscribe --topic-arn "$sns_topic_arn" --protocol sqs --notification-endpoint "$sqs_endpoint_arn"
echo "SQS queue subscribed to SNS topic"

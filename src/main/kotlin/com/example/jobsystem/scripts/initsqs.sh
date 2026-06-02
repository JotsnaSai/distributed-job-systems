#!/bin/bash

echo "Creating DLQ..."
awslocal sqs create-queue \
  --queue-name job-queue-dlq \
  --attributes '{
    "VisibilityTimeout": "30",
    "MessageRetentionPeriod": "86400"
  }'

echo "Getting DLQ ARN..."
DLQ_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url http://localhost:4566/000000000000/job-queue-dlq \
  --attribute-names QueueArn \
  --query 'Attributes.QueueArn' \
  --output text)

echo "Creating main queue linked to DLQ..."
awslocal sqs create-queue \
  --queue-name job-queue \
  --attributes '{
    "VisibilityTimeout": "30",
    "MessageRetentionPeriod": "86400",
    "RedrivePolicy": "{\"deadLetterTargetArn\":\"'"$DLQ_ARN"'\",\"maxReceiveCount\":\"3\"}"
  }'

echo "✅ Queues created successfully!"
echo "DLQ ARN: $DLQ_ARN"
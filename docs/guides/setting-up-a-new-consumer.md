# Setting up a new consumer

To enable a consumer to access event, they need:

- Access to [hmpps-integration-api](https://github.com/ministryofjustice/hmpps-integration-api/tree/main)
- SQS and SNS subscriber setup as per [Create new consumer subscriber queue for events](https://github.com/ministryofjustice/hmpps-integration-api/blob/main/docs/guides/setting-up-a-new-consumer.md#create-new-consumer-subscriber-queue-for-events)

for each environment they want to access.

## Setup helm config to load client SQS details from kubernetes secrets

To do this you will need the client name that you used when setting up the queue for the consumer in [Create new consumer subscriber queue for events](https://github.com/ministryofjustice/hmpps-integration-api/blob/main/docs/guides/setting-up-a-new-consumer.md#create-new-consumer-subscriber-queue-for-events). In the following instructions, replace `<client>` with your client's name, and `<CLIENT>` with the client's name in upper case. It is important that `<CLIENT>` is a case-insensitive match for `<client>`.

1. In `values.yml`, add a section like the following under `namespace_secrets`
```
event-<client>-queue:
   <CLIENT>_QUEUE_NAME: "sqs_name"
   <CLIENT>_QUEUE_ARN: "sqs_arn"
   <CLIENT>_FILTER_POLICY_SECRET_ID: "<client>_filter_policy_secret_id"
```
this will create environment variables named after the keys containing the values set in the secrets for the elements in the values. You can confirm these are correct with `kubectl -n hmpps-integration-api-dev get secrets event-<client>-queue -o json`. In the `data` field of the output, you should see `sqs_name`, `sqs_arn` and `<client>_filter_policy_secret_id`.

2. In `application-<environment>.yml`, add a section like the following under `hmpps.sqs`,
   `queues`:
```
<client>subscriberqueue:
    queueName: ${<CLIENT>_QUEUE_NAME}
    queueArn: ${<CLIENT>_QUEUE_ARN}
    subscribeTopicId: integrationeventtopic
```
This will read in queue configuration from the secrets in `values.yml`.

3. In `application-<environment>.yml`, add a section like the following under `hmpps.secret`,
   `secrets`:
```
    <CLIENT>:
      secretId: ${<CLIENT>_FILTER_POLICY_SECRET_ID}
      queueId: <client>subscriberqueue
```
This will read in the filter configuration from the secrets in `values.yml` and also map the queueId to the queue configuration above.

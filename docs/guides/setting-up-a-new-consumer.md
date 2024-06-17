# Setting up a new consumer

To enable a consumer to access event, they need:

- Access to [hmpps-integration-api](https://github.com/ministryofjustice/hmpps-integration-api/tree/main)
- SQS and SNS subscriber setup as per [Create new consumer subscriber queue for events](https://github.com/ministryofjustice/hmpps-integration-api/blob/main/docs/guides/setting-up-a-new-consumer.md#create-new-consumer-subscriber-queue-for-events)

per environment.

## Setup helm config load client SQS details from kubernetes secrets

To enable event service to manage client subscriptions, the follow settings are required for each client in [values.yaml](..%2F..%2Fhelm_deploy%2Fhmpps-integration-events%2Fvalues.yaml). The value of settings should be stored in kubernetes when the infrastructure are created by Terraform:

Replace clientCNname with the Common Name(CN) name of client certificate created in the [Setting up a new consumer](https://github.com/ministryofjustice/hmpps-integration-api/blob/main/docs/guides/setting-up-a-new-consumer.md) step. 

Replace client name with name of the client(no space).
```bash
  HMPPS_SECRET_SECRETS_[clientCDNname]_QUEUE-NAME: "[clientname]QUEUE"
  HMPPS_SQS_QUEUES_[clientname]QUEUE_QUEUENAME: "[key of kubernetes secret]"
  HMPPS_SQS_QUEUES_[clientname]QUEUE_QUEUEARN: "[key of kubernetes secret]"
  HMPPS_SQS_QUEUES_[clientname]QUEUE_SUBSCRIBETOPICID: "INTEGRATIONEVENTTOPIC"
  HMPPS_SECRET_SECRETS_[clientCDNname]_SECRETID: "[key of kubernetes secret]"
```

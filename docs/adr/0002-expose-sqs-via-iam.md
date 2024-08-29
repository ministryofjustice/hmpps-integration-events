# 0002 - Expose access to SQS queues using IAM

2024-08-29

## Status

Accepted

## Context

Clients currently access the API endpoints provided by [hmpps-integration-api](https://github.com/ministryofjustice/hmpps-integration-api), using a combination of Mutual TLS and an API key.

Ideally, we would like clients to use the same authentication mechanism for accessing SQS queues.


Options:
1. **Provide an API wrapper around SQS**.
   This means we provide an implementation-agnostic interface, however it would be non-standard and requires more
   work to wrap all the options that the AWS APIs provide. 
2. **Provide IAM access keys for accessing SQS directly**.
   Generate an IAM user account for each client and share long-lived access keys with them with access only to their
   queue. This would allow clients to access the SQS queue directly via the AWS APIs, but has the risk of tokens being
   leaked or shared. It also means token rotation would need to be a joint effort between HMPPS and each client.
3. **Provide an API endpoint for clients to retrieve temporary IAM access keys**.
   Add an API Gateway endpoint that returns temporary access keys. This has all the same benefits of option 2, but means
   clients can use the existing authentication mechanism we already have in place for API Gateway (i.e. mTLS + API Key).

## Decision

Provide an API endpoint for clients to retrieve temporary IAM access keys

## Consequences

- Clients can use their existing certificates and API keys for accessing the queue
- Clients can use the SQS APIs or SDKs to access their queue
- Permissions can be managed via IAM
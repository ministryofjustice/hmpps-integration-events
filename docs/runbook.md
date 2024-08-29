# HMPPS Integration API Runbook

This is a runbook to document how this service is supported, as described in: [MOJ Runbooks](https://technical-guidance.service.justice.gov.uk/documentation/standards/documenting-how-your-service-is-supported.html#what-you-should-include-in-your-service-39-s-runbook)

Last review date: 10/06/2024 **Currently only deployed to Dev this will need updating as deployed further**

## About the service

This service triggers SNS notifications by processing upstream MoJ domain events which are related to the information served by the [hmpps-integration-api](https://github.com/ministryofjustice/hmpps-integration-api). This allows the clients of our API to be notified when a change occurs to a domain that is of interest to them.

### Tech stack

#### Ran on cloud platform

- Containerised Kotlin Spring Boot application running on Cloud Platform’s Kubernetes cluster (eu-west-2).
- AWS SQS
- AWS SNS
- AWS S3
- AWS RDS Postgres DB
- AWS secrets manager

## Incident response hours

Office hours, usually 9am-6pm on working days.

## Incident contact details

[hmpps-integration-api@digital.justice.gov.uk](mailto:hmpps-integration-api@digital.justice.gov.uk)

## Service team contact

The service team can be reached on MOJ Slack: [#ask-hmpps-integration-api](https://moj.enterprise.slack.com/archives/C04D46K9QTU)


## Other URLs

### Application source code

https://github.com/ministryofjustice/hmpps-integration-events

### Documentation

Source: https://github.com/ministryofjustice/hmpps-integration-events/tree/main/docs

### Cloud platform infrastructure as code

- [Development](https://github.com/ministryofjustice/cloud-platform-environments/tree/main/namespaces/live.cloud-platform.service.justice.gov.uk/hmpps-integration-api-dev)

## Expected speed and frequency of releases

Trunk based development and continuous integration is practiced on this service. If changes pass all automated tests, they are currently deployed all the way to dev.
There is no change request process and the delivery team pushes to production regularly.

## Impact of an outage

Since we have a variety of consumers, the impact will be different for each of them. In all cases it would prevent civil servants from doing their work and the impact would be quite significant.

## Restrictions on access

Consumers need to be onboard and go through a mutual TLS authentication. They also need to send a pre-shared key (AWS API Gateway API Key) as a header for identification before being allowed to access the service.

There are no IP restrictions in place.

# hmpps-integration-events

[![CircleCI](https://dl.circleci.com/status-badge/img/gh/ministryofjustice/hmpps-integration-events/tree/main.svg?style=svg)](https://dl.circleci.com/status-badge/redirect/gh/ministryofjustice/hmpps-integration-events/tree/main)
[![repo standards badge](https://img.shields.io/badge/endpoint.svg?&style=flat&logo=github&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-integration-events)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-report/hmpps-integration-events "Link to report")
[![Docker Repository on Quay](https://img.shields.io/badge/quay.io-repository-2496ED.svg?logo=docker)](https://quay.io/repository/hmpps/hmpps-integration-events)

## Contents

- [About this project](#about-this-project)
  - [Technologies](#technologies)
  - [External Dependencies](#external-dependencies)
- [Runbook](#runbook)
- [Get started locally](#get-started-locally)
  - [local dependencies](#local-dependencies)
  - [Using IntelliJ IDEA](#using-intellij-idea)
  - [Running the tests](#running-the-tests)
  - [Running the linter](#running-the-linter)
  - [Running all checks](#running-all-checks)
- [Further documentation](#further-documentation)
- [Related repositories](#related-repositories)
- [License](#license)

## About this project

A Kotlin Spring boot application which triggers SNS notifications by processing upstream MoJ domain events which are related to the information served by the [hmpps-integration-api](https://github.com/ministryofjustice/hmpps-integration-api). This allows the clients of our API to be notified when a change occurs to a domain that is of interest to them.

### The role of this service

The events served from this service are primarily intended to be used to let you know when to invalidate a cache. The events are:
- Minimal - they do not contain data beyond what is needed to refresh your cache (e.g. the URL).
- Mapped 1 to 1 with API endpoints on [HMPPS Integration API](https://github.com/ministryofjustice/hmpps-integration-api). An event is only provided to you if you have access to the corresponding endpoint.

For example, if you have access to the "Get a person's name" endpoint (`v1/persons/{hmppsId}/name`), and a new person is created in the upstream systems, you will receive a `PERSON_NAME_CHANGED` event for an HMPPS ID you have not seen before. You will not receive a new person event.

This also means that if you have access to multiple endpoints, a single action (such as a new person being created in the system) may result in you receiving multiple events.

### How it works

At a high level, this service 
1. Listens for HMPPS Domain Events
2. Transforms them into HMPPS Integration Events
3. Puts the Integration Event on the Integration Event Topic

Consumers who want to receive Integration Events, [will need an SQS queue and Subscription to the Integration Events Topic created](https://github.com/ministryofjustice/hmpps-integration-api/blob/main/docs/guides/setting-up-a-new-consumer.md#create-new-consumer-subscriber-queue-for-events). This will provide them with a queue that receives events when they are put on the Integration Event topic

This project has three asynchronous processes:

#### 1. Update filter policies - Every hour

To restrict the events that a consumer receives, the SNS subscription filter policy for each queue is updated every hour. To do this, we 
1. Call the Integration API's config method to receive the updated consumer configurations.
2. Update the SNS subscription filter policy.  

We set the filter policies to only allow the events that 
- Correspond to endpoints they have access to.
- Match the filters they have on the Integration API (if any).

#### 2. Listen for HMPPS Domain Events

Whenever a Domain event is received, we convert it to the corresponding Integration Events and insert them into the database (a single Domain event can cause multiple Integration Events). In the case that the new Integration Event is a duplicate, we update the existing event. 

#### 3. Send HMPPS Integration Events - Every 10 seconds

Search the database for events older than 5 minutes. Add them to the Integration Events Topic and then delete them.

### Technologies

- [Cloud Platform](https://user-guide.cloud-platform.service.justice.gov.uk/#cloud-platform-user-guide) - Ministry of
  Justice's (MOJ) cloud hosting platform built on top of AWS which offers numerous tools such as logging, monitoring and
  - [AWS](https://aws.amazon.com/) - Services utilise AWS features through Cloud Platform such
    as:
    - [Simple Queue Service (SQS)](https://aws.amazon.com/sqs/) to consume and persist incoming AWS SNS notifications.
    - [Simple Notification Service (SNS)](https://aws.amazon.com/sns/) to emit notifications to our consumers.
    - [Simple Storage Service (S3)](https://aws.amazon.com/s3/) to hold client certificates.
    - [Secrets Manager](https://aws.amazon.com/secrets-manager/) to manage authorisation.
    - [Relational Database Service (RDS)](https://aws.amazon.com/rds/) to manage state to avoid creating duplicate notifications.
- [CircleCI](https://circleci.com/developer) - Used for our build platform, responsible for executing workflows to
  build, validate, test and deploy our project.
- [Docker](https://www.docker.com/) - The API is built into docker images which are deployed to our containers.
- [Kubernetes](https://kubernetes.io/docs/home/) - Creates 'pods' to host our environment. Manages auto-scaling, load
  balancing and networking to our application.

### External Dependencies

- [hmpps-domain-events](https://github.com/ministryofjustice/hmpps-domain-events)

## Runbook

The [runbook](https://github.com/ministryofjustice/hmpps-integration-events/tree/main/docs/runbook.md) for this application can be found in the docs folder of this repo.

## Get started locally

1. Enable pre-commit hooks for formatting, linting, and secret scanning.

   ```
    # Install pipx if not already installed
    brew install pipx
    # Ensure the path to pipx-installed tools is active
    pipx ensurepath
    # Restart your terminal after running this
    # Install pre-commit
    pipx install pre-commit
    # Install hooks into .git/hooks
    pre-commit install
   ```
### Local dependencies

- [localstack](https://www.localstack.cloud/) a cloud software development framework to emulate AWS services.
- [docker](https://www.docker.com/) used to manage virtual application containers on a common OS to ease development.

### Using IntelliJ IDEA

When using an IDE like [IntelliJ IDEA](https://www.jetbrains.com/idea/), getting started is very simple as it will
handle installing the required Java SDK and [Gradle](https://gradle.org/) versions. The following are the steps for
using IntelliJ but other IDEs will prove similar.

1. Clone the repo.

    ```bash
    git clone git@github.com:ministryofjustice/hmpps-integration-events.git
    ```

2. Launch IntelliJ and open the `hmpps-integration-events` project by navigating to the location of the repository. Upon opening the project, IntelliJ will begin downloading and installing necessary dependencies which may take a few
   minutes.
3. Run `make create-env-file` to generate a `.env` file containing random values that will be set on the local dependencies and in the application config. Ensure that this file **does not** get commited.

4. Obtain an API key for [hmpps-integration-api](https://github.com/ministryofjustice/hmpps-integration-api/tree/main) and set in [application-localstack.yml](src%2Fmain%2Fresources%2Fapplication-localstack.yml)
5. Ensuring that docker is running within the root folder of the codebase, run the following command.

    ```bash
    make serve
    ```
    
    This will spin up the Spring Boot events application, Postgres DB and Localstack containers within Docker. For ease of development it may be easier to run the Spring Boot events application in IntelliJ to avoid having to spin up and down containers when you make changes. This can be done by creating a configuration file in IntelliJ with an active profile set as `localstack` and clicking the run button in the IDE.

### Running the tests

To run unit tests using the command line:

```bash
make unit-test
```

### Running the linter

To lint the code using [Ktlint](https://pinterest.github.io/ktlint/):

```bash
make lint
```

To autofix any styling issues with the code:

```bash
make format
```

### Running all checks

To run all the tests and linting:

```bash
make check
```

## Developer guides

- [Setting up a new consumer](docs%2Fguides%2Fsetting-up-a-new-consumer.md)

## Further documentation

- [Architecture Decision Records (ADRs)](/docs/adr)
- [Architecture diagrams](/docs/diagrams)

## Related repositories

| Name                                                                                          | Purpose                                                                            |
| --------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------- |
| [hmpps-integration-api](https://github.com/ministryofjustice/hmpps-integration-api/tree/main) | A long-lived API that exposes data from HMPPS systems.                             |
| [hmpps-domain-events](https://github.com/ministryofjustice/hmpps-domain-events)               | Captures and broadcasts the occurrence of significant activity in an HMPPS domain. |

## License

[MIT License](LICENSE)

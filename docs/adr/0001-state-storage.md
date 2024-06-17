# 0001 - State Storage

2024-03-05

## Status

Accepted

## Context

State storage is essential for maintaining a record of processed events, ensuring accurate event processing, and avoiding duplicate notifications in our system.
It also plays a crucial role in resilience and fault tolerance, as it allows us to recover and resume operations from system failures or downtime by providing an accurate understanding of the system's state.
As a result we need to decide which state storage system we should use.

1. Cloud Platform RDS (PostgreSQL)
2. Cloud Platform DynamoDB
3. Elasticache/Redis

The DB will be used in roughly the following way:

1. **Get Event**: Retrieve events from the SQS queue.
2. **Process Event**: Analyze the incoming event to determine its type and contents.
3. **Check State**: Check the state storage (e.g., a database) to see if a similar event has already been processed. This check involves comparing the incoming event with the stored state to identify duplicates.
4. **Save or Clear Event**:
   - If the event is not a duplicate, save it to the database for future reference. This step ensures that the system keeps track of processed events and avoids duplicates.
   - If it is a duplicate then update the last modified date for the specified record.
   - If a similar event has already been processed, clear it from the SQS queue to avoid redundant processing and notifications.
5. **Notification Generation**:
   - Periodically, our notifier code checks the state storage for events that haven't been processed yet or have been processed but not yet notified.
   - Generate notifications based on the events found in the state storage. This step involves formatting the events into appropriate notifications for our consumers.
6. **Clear State After Notification**: After successfully sending notifications for processed events, clear the corresponding entries from the state storage. This ensures that the system maintains an up-to-date record of events and avoids unnecessary storage overhead.

## Decision: Cloud PLatform RDS (PostgreSQL)

We decided that for our use case either RDS or DynamoDB could work as neither Dynamos superior speed nor PostgreSQL's superior querying are relevant to our use case.
As a result we decided to go for the PostgreSQL because:

- Better Cloud Platform support
- More Cloud Platform and MoJ examples
- Recommendation from MoJ stakeholders
- Current development team knowledge is more advanced with PostgreSQL
- Easier handover to maintainers

## Consequences

- Our DB Infrastructure will be Cloud Platform RDS.
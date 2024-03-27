CREATE SCHEMA event_store;

CREATE TABLE event_store.event_notifications(
    EVENT_ID SERIAL PRIMARY KEY,
    HMPPS_ID varchar(15),
    EVENT_TYPE varchar(20),
    URL varchar(200),
    LAST_MODIFIED_DATETIME TIMESTAMP NOT NULL
);
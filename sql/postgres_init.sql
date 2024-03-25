CREATE SCHEMA event_store;

CREATE TABLE event_store.event_notifications(
    event_id SERIAL PRIMARY KEY,
    hmpps_id varchar(15),
    event_type varchar(20),
    url varchar(200)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_event_notification_url_event_type_status ON EVENT_NOTIFICATION(URL, EVENT_TYPE, STATUS);

DROP INDEX IF EXISTS idx_event_notification_url_event_type;
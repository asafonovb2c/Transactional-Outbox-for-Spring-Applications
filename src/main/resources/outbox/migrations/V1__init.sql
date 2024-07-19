CREATE SCHEMA IF NOT EXISTS outbox;

CREATE TABLE IF NOT EXISTS outbox.event (
    uuid             UUID                NOT NULL PRIMARY KEY,
    event_type       text                            NOT NULL,
    create_timestamp timestamp with time zone        NOT NULL,
    run_time         timestamp with time zone        NOT NULL,
    event            text                            NOT NULL,
    attempts         integer                         NOT NULL,
    fail_reason      text,
    lock_key         text                            NOT NULL,
    status           text                            NOT NULL
);

COMMENT ON TABLE outbox.event IS 'Event queue';
COMMENT ON COLUMN outbox.event.uuid IS 'Event ID';
COMMENT ON COLUMN outbox.event.event_type IS 'Queue type';
COMMENT ON COLUMN outbox.event.create_timestamp IS 'Event creation time';
COMMENT ON COLUMN outbox.event.run_time IS 'Time for updating the event';
COMMENT ON COLUMN outbox.event.attempts IS 'Number of retry attempts';
COMMENT ON COLUMN outbox.event.event IS 'Event text';
COMMENT ON COLUMN outbox.event.fail_reason IS 'Reason for processing failure';
COMMENT ON COLUMN outbox.event.lock_key IS 'Lock key';
COMMENT ON COLUMN outbox.event.status IS 'Event status';

CREATE INDEX  IF NOT EXISTS outbox_event__event_type__run_time_idx
    ON outbox.event USING btree (event_type, run_time);

CREATE INDEX  IF NOT EXISTS outbox_event__event_type__unique_id_idx
    ON outbox.event USING btree (event_type, uuid);
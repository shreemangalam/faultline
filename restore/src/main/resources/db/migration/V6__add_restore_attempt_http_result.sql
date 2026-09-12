ALTER TABLE restore_event_attempt ADD COLUMN http_status INTEGER;
ALTER TABLE restore_event_attempt ADD COLUMN duration_ms BIGINT;

ALTER TABLE restore_event_attempt
    ADD CONSTRAINT restore_event_attempt_http_status_check
    CHECK (http_status IS NULL OR http_status BETWEEN 100 AND 599);

ALTER TABLE restore_event_attempt
    ADD CONSTRAINT restore_event_attempt_duration_check
    CHECK (duration_ms IS NULL OR duration_ms >= 0);

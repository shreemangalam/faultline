CREATE TABLE restore_event_attempt (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES restore_event(id),
    attempt_number INTEGER NOT NULL,
    worker_id VARCHAR(100) NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    failure_reason VARCHAR(500),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT restore_event_attempt_number_unique UNIQUE (event_id, attempt_number)
);

CREATE INDEX restore_event_attempt_event_idx ON restore_event_attempt (event_id, attempt_number);

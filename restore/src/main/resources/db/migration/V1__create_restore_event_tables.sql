CREATE TABLE restore_event (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    request_fingerprint VARCHAR(512) NOT NULL,
    correlation_id VARCHAR(100) NOT NULL,
    target_system VARCHAR(100) NOT NULL,
    payload_reference VARCHAR(255) NOT NULL,
    priority INTEGER NOT NULL,
    deadline_at TIMESTAMP WITH TIME ZONE,
    state VARCHAR(32) NOT NULL,
    attempt_count INTEGER NOT NULL,
    next_eligible_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX restore_event_state_eligible_idx ON restore_event (state, next_eligible_at, priority, created_at);

CREATE TABLE restore_event_audit (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES restore_event(id),
    to_state VARCHAR(32) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX restore_event_audit_event_idx ON restore_event_audit (event_id, occurred_at);
CREATE TABLE restore_event_dependency (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES restore_event(id),
    prerequisite_event_id UUID NOT NULL REFERENCES restore_event(id),
    CONSTRAINT restore_event_dependency_not_self CHECK (event_id <> prerequisite_event_id),
    CONSTRAINT restore_event_dependency_unique UNIQUE (event_id, prerequisite_event_id)
);

CREATE INDEX restore_event_dependency_prerequisite_idx ON restore_event_dependency (prerequisite_event_id);
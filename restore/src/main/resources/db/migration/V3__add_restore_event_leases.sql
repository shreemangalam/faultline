ALTER TABLE restore_event ADD COLUMN lease_owner VARCHAR(100);
ALTER TABLE restore_event ADD COLUMN lease_expires_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX restore_event_claimable_idx ON restore_event (state, next_eligible_at, lease_expires_at, priority, created_at);
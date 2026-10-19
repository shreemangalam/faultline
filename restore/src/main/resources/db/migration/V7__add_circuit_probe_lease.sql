ALTER TABLE restore_circuit_breaker ADD COLUMN probe_event_id UUID;
ALTER TABLE restore_circuit_breaker ADD COLUMN probe_lease_expires_at TIMESTAMP WITH TIME ZONE;

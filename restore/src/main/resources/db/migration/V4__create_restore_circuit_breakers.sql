CREATE TABLE restore_circuit_breaker (
    target_system VARCHAR(100) PRIMARY KEY,
    state VARCHAR(32) NOT NULL,
    consecutive_failures INTEGER NOT NULL,
    open_until TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
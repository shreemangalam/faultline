# Restore durable event foundation

- Added PostgreSQL/H2-compatible Flyway tables for recovery events and lifecycle audit entries, with explicit `ELIGIBLE` state after receipt.
- Added validated submit, retrieve, and state-filtered queue APIs. An idempotency key returns the existing matching event and rejects changed input.
- Added Restore to Compose on port 8082. Verified four focused tests and a live PostgreSQL event create/retrieve round-trip with two audit entries.
- Added persisted event prerequisites. A child with any incomplete prerequisite is recorded as `BLOCKED`; duplicate, missing, or cross-correlation prerequisites are rejected. Dependency IDs participate in the idempotency fingerprint. Releasing a blocked event remains deliberately deferred to the worker-transition slice.
- Restore uses its own Flyway history table in the shared demo database; worker leasing, retries, circuit health, and the downstream simulator remain the next increments.
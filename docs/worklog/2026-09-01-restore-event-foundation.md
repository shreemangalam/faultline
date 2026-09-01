# Restore durable event foundation

- Added PostgreSQL/H2-compatible Flyway tables for recovery events and lifecycle audit entries, with explicit `ELIGIBLE` state after receipt.
- Added validated submit, retrieve, and state-filtered queue APIs. An idempotency key returns the existing matching event and rejects changed input.
- Added Restore to Compose on port 8082. Verified four focused tests and a live PostgreSQL event create/retrieve round-trip with two audit entries.
- Restore uses its own Flyway history table in the shared demo database; dependency relations, worker leasing, retries, circuit health, and the downstream simulator remain the next increments.
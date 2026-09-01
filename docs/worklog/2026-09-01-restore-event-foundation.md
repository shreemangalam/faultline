# Restore durable event foundation

- Added PostgreSQL/H2-compatible Flyway tables for recovery events and lifecycle audit entries, with explicit `ELIGIBLE` state after receipt.
- Added validated submit, retrieve, and state-filtered queue APIs. An idempotency key returns the existing matching event and rejects changed input.
- Added Restore to Compose on port 8082. Verified four focused tests and a live PostgreSQL event create/retrieve round-trip with two audit entries.
- Added persisted event prerequisites. A child with any incomplete prerequisite is recorded as `BLOCKED`; duplicate, missing, or cross-correlation prerequisites are rejected. Dependency IDs participate in the idempotency fingerprint. Releasing a blocked event remains deliberately deferred to the worker-transition slice.
- Added a bounded worker claim API backed by PostgreSQL-compatible `FOR UPDATE SKIP LOCKED`. Claims persist a visibility lease and audit entry; an expired lease is reclaimable by another worker, providing at-least-once recovery after a worker crash.
- Added lease-owner-guarded completion. Completing a prerequisite transactionally releases only dependants whose full prerequisite set is complete.
- Added bounded retry scheduling and poison-event exhaustion after three attempts.
- Added persisted target circuits with open-circuit claim suppression, a single half-open probe, and success reset.
- Restore uses its own Flyway history table in the shared demo database; jitter, failure classification, attempt details, and the downstream simulator remain next.
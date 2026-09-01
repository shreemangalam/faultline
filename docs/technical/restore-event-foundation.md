# Restore durable event foundation

Restore accepts durable recovery events through `POST /api/v1/events`, returns an existing event for an identical idempotent submission, and rejects a reused key with different business input. `GET /api/v1/events/{eventId}` returns persisted state, prerequisite IDs, and audit history; `GET /api/v1/events?status=ELIGIBLE` exposes the initial operational queue.

An event may name up to 20 prerequisite event IDs. Prerequisites must exist, be unique, and use the same correlation ID. If any prerequisite is not `COMPLETED`, the new event is persisted as `BLOCKED` with an audit entry; otherwise it is `ELIGIBLE`. Prerequisite membership is part of the idempotency fingerprint, so reusing a key with changed dependencies is rejected. This increment records the gate only: a future worker/transition slice will release blocked events after prerequisites complete.

Survey and Restore currently share the local PostgreSQL database for an easy Compose demo. Restore uses a separate Flyway history table and baselines at version `0` before applying its own migrations, which lets it migrate alongside Survey without treating Survey tables as its schema history.

Run the full local stack with `docker compose -f infra/compose.yaml up --build`; Restore health is at `http://localhost:8082/actuator/health`.
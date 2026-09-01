# Restore durable event foundation

Restore accepts durable recovery events through `POST /api/v1/events`, returns an existing event for an identical idempotent submission, and rejects a reused key with different business input. `GET /api/v1/events/{eventId}` returns persisted state, prerequisite IDs, lease details, and audit history; `GET /api/v1/events?status=ELIGIBLE` exposes the operational queue.

An event may name up to 20 prerequisite event IDs. Prerequisites must exist, be unique, and use the same correlation ID. If any prerequisite is not `COMPLETED`, the new event is persisted as `BLOCKED` with an audit entry; otherwise it is `ELIGIBLE`. Prerequisite membership is part of the idempotency fingerprint, so reusing a key with changed dependencies is rejected. A successful worker completion releases a blocked dependant only after every persisted prerequisite is `COMPLETED`.

A worker calls `POST /api/v1/worker/claims` with a worker ID, batch size, and lease duration. Restore selects due eligible events and expired `IN_PROGRESS` leases with a transactional `FOR UPDATE SKIP LOCKED` query, then persists `IN_PROGRESS`, the lease owner and expiry, an incremented attempt count, and an audit entry before returning work. This is at-least-once processing: a crashed worker’s lease becomes visible for another worker to reclaim. The lease-owning worker completes work through `POST /api/v1/worker/claims/{eventId}/complete`; a stale or different worker is rejected. Retry and terminal-failure transitions remain next.

Survey and Restore currently share the local PostgreSQL database for an easy Compose demo. Restore uses a separate Flyway history table and baselines at version `0` before applying its own migrations, which lets it migrate alongside Survey without treating Survey tables as its schema history.

Run the full local stack with `docker compose -f infra/compose.yaml up --build`; Restore health is at `http://localhost:8082/actuator/health`.
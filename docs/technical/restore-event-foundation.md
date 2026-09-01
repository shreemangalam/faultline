# Restore durable event foundation

Restore now accepts durable recovery events through `POST /api/v1/events`, returns an existing event for an identical idempotent submission, and rejects a reused key with different business input. `GET /api/v1/events/{eventId}` returns the persisted state and audit history; `GET /api/v1/events?status=ELIGIBLE` exposes the initial operational queue.

Each accepted event is immediately eligible because this slice has no prerequisite relation yet. The audit records both receipt and eligibility, so later dependency, retry, and worker transitions extend an existing history rather than inventing one.

Survey and Restore currently share the local PostgreSQL database for an easy Compose demo. Restore uses a separate Flyway history table and baselines at version `0` before applying its own migrations, which lets it migrate alongside Survey without treating Survey tables as its schema history.

Run the full local stack with `docker compose -f infra/compose.yaml up --build`; Restore health is at `http://localhost:8082/actuator/health`.
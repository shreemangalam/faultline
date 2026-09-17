# Restore live updates

- Added `GET /api/v1/stream` with an initial readiness event, committed recovery-state events, proxy-buffering protection, configurable timeout, and heartbeat comments.
- Published every durable event transition through a transaction-aware listener so rolled-back writes cannot reach the dashboard.
- Moved recovery-event delivery onto a bounded single-thread dispatcher. Slow clients cannot delay committed mutation responses; retained invalidations remain ordered, and overload discards the oldest queued invalidation.
- Kept reconnection deliberately snapshot-based: SSE is a process-local notification path and PostgreSQL-backed REST remains authoritative.
- Verified three focused dispatcher/SSE tests and the full 66-test reactor.
- Recreated the lifecycle against OpenJDK 21.0.12 and PostgreSQL 16.15: `stream-ready`, `ELIGIBLE`, `IN_PROGRESS`, and `COMPLETED` arrived in order, the final REST snapshot was `COMPLETED`, and the disposable proof stack was removed.
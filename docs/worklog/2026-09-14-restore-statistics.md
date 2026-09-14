# Restore operational statistics

- Added `GET /api/v1/statistics` as the persisted aggregate contract for the Restore console and load harness.
- The response includes every event and attempt state, zero-filled counts, pending and terminal-failure totals, and nearest-rank p95 latency from event receipt to successful completion.
- Aggregate queries select grouped counts and completion timestamps rather than loading complete event and attempt records.
- Unit coverage fixes the percentile definition; an HTTP integration test proves the aggregates against real successful and transient downstream executions.
- Verified 35 focused Restore tests and the full 57-test Maven reactor. A rebuilt Java 21 container reported the unchanged 22-event PostgreSQL queue, five attempt rows, and a 317 ms successful-recovery p95; Survey, Restore, and the dashboard remained healthy.

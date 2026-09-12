# Restore execution

- Added the lease-owner-guarded `FAILED_FINAL` transition for non-retriable work.
- Permanent failure closes the active attempt as `PERMANENT_FAILURE`, clears the lease, and does not increment the target circuit because validation defects are not evidence of target instability.
- Verified 12 focused Restore tests, the full 34-test Maven reactor, a rebuilt Restore container, and a live PostgreSQL transition with the target circuit remaining closed at zero failures.
- Added a controllable HTTP downstream simulator. Success is idempotent per target and key; repeatable target configuration exposes realistic transient, rate-limit, delayed, and permanent responses.
- Verified 15 focused Restore tests, the full 37-test Maven reactor, and live container HTTP behavior for first acceptance, duplicate acceptance, counters, and `503` simulation.
- Added a bounded real-HTTP executor at `POST /api/v1/worker/run-once`. It performs network I/O after the short claim transaction, applies explicit timeouts and response classification, and persists HTTP status and duration with each attempt.
- Preserved simulator acceptance history across behavior changes by default. A real-server integration test proves that a timed-out request can commit downstream and later replay as duplicate-success with one accepted side effect.
- Verified 20 focused Restore tests, the full 42-test Maven reactor, a rebuilt container with Flyway V6, and a live PostgreSQL-backed execution: one request, one accepted side effect, `COMPLETED`, HTTP `200`, and persisted duration.
- Added explicit terminal dependency propagation. Existing blocked descendants and new dependants of `EXHAUSTED`, `FAILED_FINAL`, or `FAILED_DEPENDENCY` events fail fast with an audited `FAILED_DEPENDENCY` state.
- Verified 21 focused Restore tests, the full 43-test Maven reactor, a rebuilt container, and a live PostgreSQL chain: `FAILED_FINAL` propagated through two generations with zero child attempts, while a later dependant failed immediately.
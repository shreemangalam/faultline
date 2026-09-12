# Restore execution

- Added the lease-owner-guarded `FAILED_FINAL` transition for non-retriable work.
- Permanent failure closes the active attempt as `PERMANENT_FAILURE`, clears the lease, and does not increment the target circuit because validation defects are not evidence of target instability.
- Verified 12 focused Restore tests, the full 34-test Maven reactor, a rebuilt Restore container, and a live PostgreSQL transition with the target circuit remaining closed at zero failures.
- Added a controllable HTTP downstream simulator. Success is idempotent per target and key; repeatable target configuration exposes realistic transient, rate-limit, delayed, and permanent responses.
- Verified 15 focused Restore tests, the full 37-test Maven reactor, and live container HTTP behavior for first acceptance, duplicate acceptance, counters, and `503` simulation.
- Automatic HTTP execution and outcome classification remain the next slice.

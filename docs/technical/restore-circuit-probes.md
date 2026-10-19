# Restore circuit probe lifecycle

Circuits are persisted per target. Three consecutive transient failures open a closed circuit for 30 seconds. After cooldown, one admitted event becomes the half-open probe. Its event UUID and worker lease deadline are persisted in `restore_circuit_breaker` by migration V7.

While that lease is valid, other events for the target cannot be claimed. When it expires, another claim may replace the abandoned probe; the previous event is eligible for ordinary expired-lease recovery. Existing pre-V7 half-open circuits with no probe deadline can also admit a replacement. Row locking on the circuit makes replacement admission exclusive.

| Probe outcome | Event result | Target result |
|---|---|---|
| Success | `COMPLETED` | Close; reset transient failure count |
| Transient response or transport failure | Retry or exhaust according to attempt policy | Reopen for 60 seconds |
| Permanent HTTP rejection | `FAILED_FINAL`; propagate dependency failure | Close: the endpoint responded, and invalid input must not strand unrelated work |
| Worker disappears | Remains leased until expiry | A replacement probe can be admitted after expiry |

Only the recorded probe event can settle a half-open circuit. A late result from work admitted before the circuit opened cannot settle somebody else's probe. Success from an older batch also cannot bypass an open circuit's cooldown. Worker transition calls still require matching ownership and an unexpired event lease.

A permanent rejection establishes reachability under this demo policy, not complete application health. Inspect the rejected event's HTTP evidence. A circuit is an admission policy based on observed execution, not continuous target monitoring.

Claimed batches execute sequentially within each worker request. Keep the lease longer than the expected total batch execution duration, including connection/read timeouts and database work. The defaults are five events, a 60-second lease, and one/two-second connect/read timeouts. Changing timeouts or batch sizes requires revisiting that budget. Expired attempts are recorded as `LEASE_EXPIRED`; the three-attempt stop rule is applied to transient-failure transitions, not a universal cap on worker crashes.

`RecoveryWorkerControllerTest` verifies exclusive probes, expired-probe replacement, rejection of the abandoned worker, permanent-probe settlement, and protection from unrelated late results. `RetryRecoveryEventTest` and operations scenario tests cover the ordinary open/probe/success path.

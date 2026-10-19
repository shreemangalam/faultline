# Restore terminal-failure contract

Restore stops work instead of retrying indefinitely. Terminal records remain in PostgreSQL with their attempt and audit history, and operators can query them through `GET /api/v1/events?status=FAILED_FINAL`, `EXHAUSTED`, or `FAILED_DEPENDENCY`.

| Failure | Classification | Terminal result | Circuit effect |
|---|---|---|---|
| Non-retriable downstream response, such as HTTP 422 | Permanent | `FAILED_FINAL` after one attempt | No failure penalty; if this event owns the half-open probe, the reachable target closes. |
| Retriable response, such as HTTP 503 | Transient | `EXHAUSTED` on the third attempt | Counts toward target health; the third consecutive failure opens the circuit. |
| A blocked event whose prerequisite terminates | Dependency | `FAILED_DEPENDENCY` without an attempt | Propagates through every still-blocked descendant in the same transaction. |

These terminal states are Restore's durable dead-letter path. They are observable and preserved for investigation; the automatic worker never claims them. An operator redrive is intentionally a separate future command because changing a terminal decision must be explicit and auditable.

The [circuit probe contract](restore-circuit-probes.md) defines permanent-probe settlement and lease expiry. Resolving target admission does not redrive the rejected event or its failed dependants.

`POST /api/v1/scenarios/terminal-failures` loads two isolated failure paths and declares their expected terminal states and attempt counts. Its end-to-end test uses the normal HTTP executor, simulator, retry policy, circuit breaker, dependency propagation, event queries, and statistics endpoint.

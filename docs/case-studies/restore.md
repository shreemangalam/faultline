# Faultline Restore: dependency-aware event recovery

## Problem

Naive retries can execute business steps in the wrong order and overwhelm an unhealthy target. A recovery system also needs to retain evidence, survive worker loss, distinguish invalid input from an outage, and handle ambiguous remote results.

## Implementation

Restore stores events, prerequisite links, attempt outcomes, audit transitions, leases, and target circuits in PostgreSQL. Short `FOR UPDATE SKIP LOCKED` claim transactions admit bounded work; HTTP delivery occurs after claim locks are released. Completion releases dependants only when all prerequisites succeed. Terminal failure propagates to still-blocked descendants.

Explicit connect/read timeouts and response classification separate success, transient failure, and permanent rejection. Transient retries use capped exponential backoff with injectable jitter; the third transient-failure attempt exhausts. Persisted circuits contain outages and admit one half-open probe after cooldown. Probe identity/expiry allow crash recovery and prevent unrelated late results from settling another event's probe.

Stable event UUIDs serve as downstream idempotency keys. API mutations require an operator key; internal simulator delivery authenticates without implicitly forwarding the local operator credential to a separately configured target. After-commit SSE invalidations refresh the dashboard's database-backed queue, statistics, circuits, and event evidence.

## A concrete demonstration

The isolated 39-message operations scenario starts with a Salesforce outage, blocked order chains, prior successes, and two terminal failures. Recover the fictional target and run bounded batches. The recorded release walkthrough completed 24 pending events in seven batches, ending with **37 completed events, zero pending, and two retained terminal failures**. A second browser tab updated through SSE without manual refresh.

## Evidence

- [Release verification](../worklog/2026-10-18-release-readiness.md): 56 passing Restore tests, authenticated HTTP delivery, circuit-probe regressions, browser behavior, and real container execution.
- [100-event authenticated smoke benchmark](../evidence/restore-smoke-2026-10-18.json): 110 attempts, 100 accepted deliveries, zero pending/terminal events, CLOSED final circuit.
- [Historical 50,000-event benchmark](../technical/restore-recovery-benchmark.md): exact accounting and measurements tied to the September revision/environment; not a new release measurement.
- [Ordering contract](../technical/restore-delivery-ordering.md), [probe lifecycle](../technical/restore-circuit-probes.md), [terminal policy](../technical/restore-terminal-failures.md), and [security boundary](../technical/api-security.md).

## Trade-offs and boundaries

Delivery is at least once with explicit dependency partial order, not global FIFO or exactly-once remote effects. Simulator configuration and accepted-key deduplication are process-local; queue/history/circuits are durable. Batches are sequential within one request; parallel worker requests provide concurrency. SSE is bounded and process-local, and large queue-history pagination remains an extension. Terminal redrive, real SAP integration, multi-user authentication, and production hosting are outside the local version-one scope.

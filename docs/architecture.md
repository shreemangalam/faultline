# Architecture

Faultline combines landscape impact analysis with durable recovery of fictional integration events. The operator uses Survey's assessment to understand an incident and Restore's queue to recover the related business work. Creating a report does not enqueue recovery events or alter target behavior.

```text
Browser: Survey / Restore / Help
                  |
          Next.js server proxy
             /          \
      Survey API      Restore API ---- HTTP ---- downstream simulator
             \          /
          PostgreSQL tables

Restore -- after-commit SSE invalidations --> browser --> REST refresh
```

## Components and data ownership

| Component | Responsibility | Source |
|---|---|---|
| Survey | Validate/store topology, select an active import, calculate and persist incident assessments | `survey/` |
| Restore | Store events, guard dependencies and leases, execute bounded HTTP deliveries, record outcomes and target health | `restore/` |
| Dashboard | Topology inspection, incident reports, queue/evidence views, and confirmed demo controls | `dashboard/` |
| Shared | Small value objects and enums | `shared/` |

Java services use Spring Boot, JPA, and Flyway. The dashboard uses Next.js and TypeScript. In Compose both services use one PostgreSQL database, with independently prefixed tables and separate migration histories. Survey uses the default Flyway history table; Restore uses `restore_flyway_schema_history`. Both baseline at version zero when their own history is absent in an already-populated schema, then apply their migrations. Existing unrelated tables remain unchanged.

## Survey

The bundled landscape contains 16 systems and 35 directed integrations. Nodes carry business criticality; edges carry protocol, data domain, and SLA class. Protocol labels describe fictional integrations rather than implementing RFC, IDoc, SOAP, or SFTP adapters.

The import validator checks identifiers, duplicates, required metadata, criticality range, supported SLA classes, and source/target relationships. `POST /api/v1/landscape/imports` stores an immutable valid snapshot. `PUT /api/v1/landscape/imports/{importId}/activate` explicitly selects it. Graph reads use that selected snapshot or fall back to the bundled sample.

For a declared `COMPLETE_OUTAGE`, the report service:

1. Traverses outgoing integrations using breadth-first search. A visited set prevents infinite traversal; shortest-path distance gives each system's impact ring.
2. Classifies consumers as `PARTIAL` when an unaffected same-domain alternative exists, `ORPHANED` when every incoming feed is affected, or otherwise `DEGRADED`. This is a target-level summary, not measured fallback capacity.
3. Ranks affected systems using the explainable score below.
4. Uses Kahn's topological sort to return a dependency-safe recovery prefix. Lexical system IDs break ties. Unresolved nodes include cycle members and nodes still blocked behind a cycle.
5. Persists the incident, results, warnings, time, and landscape reference as an immutable report snapshot. Future topology activation does not change that report.

```text
score = criticality * 10
      + min(20, direct outgoing integration count * 2)
      + SLA points
      + max(0, 20 - (impactRing - 1) * 5)
```

SLA points are 20 for REAL_TIME, 10 for NEAR_REAL_TIME, and 0 for BATCH, using the first incoming affected integration at that node's impact ring. This is not a maximum across all incoming flows. Scores sort descending, then by system ID. Severity thresholds are CRITICAL 120+, HIGH 90+, MODERATE 60+, otherwise LOW. Consumer classification does not contribute a separate score term.

The health-map endpoint returns the selected topology with healthy baseline nodes. It is not continuous endpoint monitoring; incident assessments are hypothetical and do not update persistent health.

## Restore

Intake accepts an idempotency key, correlation ID, target, payload reference, priority 1–5, optional deadline, and up to 20 prerequisite UUIDs. Prerequisites must already exist, be unique, and share the correlation ID. Identical repeated intake returns the existing event; changed input with the same key returns 409.

```text
RECEIVED -> ELIGIBLE -> IN_PROGRESS -> COMPLETED
        \-> BLOCKED -> ELIGIBLE
IN_PROGRESS -> RETRY_SCHEDULED -> IN_PROGRESS
IN_PROGRESS -> FAILED_FINAL or EXHAUSTED
BLOCKED -> FAILED_DEPENDENCY
```

Intake records RECEIVED in audit history and returns the evaluated state. No incomplete prerequisites means ELIGIBLE; otherwise BLOCKED. An already-terminal prerequisite produces FAILED_DEPENDENCY. Completion releases a child only after every prerequisite succeeds; terminal failure propagates through still-blocked descendants.

Workers claim 1–10 events in short `FOR UPDATE SKIP LOCKED` transactions, persisting ownership, lease expiry, and attempt history. Remote I/O happens after the claim transaction. Batches execute sequentially within one request; concurrent worker requests provide parallelism. Expired leases can be reclaimed, and stale or different owners cannot finalize an event.

The HTTP client sends event UUID as the downstream idempotency key. HTTP 2xx completes; 408/425/429/5xx and transport errors are transient; other responses are permanent. Transient retry delays start at 5 and 10 seconds plus inclusive 0–2-second jitter; the third transient-failure attempt exhausts. Deadlines influence ranking rather than expiring events. Repeated worker crashes are separate from that transient-failure limit.

See the contracts for [delivery/ordering](technical/restore-delivery-ordering.md), [circuits](technical/restore-circuit-probes.md), and [terminal outcomes](technical/restore-terminal-failures.md). PostgreSQL owns the queue, dependencies, attempts, audit history, and circuits. Simulator behavior and accepted-key deduplication are process-local and reset on restart.

## Dashboard and operational limits

The server proxy forwards same-origin requests and injects credentials for mutations. Backend services own analysis and recovery policy; the UI displays their results. [SSE notifications](technical/restore-live-updates.md) invalidate REST snapshots after commit, including on reconnect; they are not a durable replay log.

The current queue response includes all event evidence, and statistics calculate successful receipt-to-completion p95 in memory. Large-history pagination, retention, distributed notifications, real business adapters, and multi-user authentication are separate extensions. Read [the security boundary](technical/api-security.md) before exposing the application beyond localhost.

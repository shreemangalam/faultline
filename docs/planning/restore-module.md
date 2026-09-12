# Faultline Restore — Dependency-Aware Event Recovery

> Working module name: **Restore**. It replaces the initial “Chronos” concept.

## Purpose

Restore is a durable retry orchestrator for failed integration events. It does more than retry in FIFO order: it waits for prerequisite events, prevents unnecessary traffic to an unhealthy downstream target, and prioritises the work that is most urgent or unblocks the most business progress.

The module simulates a dead-letter/retry process. It will not access real CPI message stores or real business systems.

## Event lifecycle

Each event has an immutable identity and correlation ID, a target system, payload reference, priority, retry policy, optional deadline, and dependency IDs. Its current status is persisted.

```text
RECEIVED -> ELIGIBLE -> IN_PROGRESS -> COMPLETED
                 |           |
                 |           +-> RETRY_SCHEDULED -> ELIGIBLE
                 |
                 +-> BLOCKED (waiting for prerequisites)

RETRY_SCHEDULED -> EXHAUSTED          (attempt limit or deadline exceeded)
IN_PROGRESS     -> FAILED_FINAL       (non-retriable failure)
BLOCKED         -> FAILED_DEPENDENCY  (a prerequisite cannot complete)
```

`BLOCKED` is non-terminal: the event is valid but cannot yet be processed safely, and it becomes eligible when all dependencies complete. `FAILED_DEPENDENCY` is terminal and records that a prerequisite can no longer complete.

## Durable design

PostgreSQL is the authoritative store. It contains event state, retry timestamps, attempt records, dependency relations, circuit-breaker state, and audit entries. This means a process restart cannot lose queued work.

A worker run claims a small batch of eligible records whose retry time has arrived. Worker claims use PostgreSQL `FOR UPDATE SKIP LOCKED` inside a short transaction, so two workers cannot claim the same event. This is more robust than relying only on an in-memory `PriorityBlockingQueue`. The current API triggers one bounded run explicitly; periodic scheduling remains a later operational layer.

An in-memory queue can later improve local ordering, but it is an optimisation, never a durability mechanism.

## Dependency graph per correlation group

Events belonging to the same business correlation, such as an order, form a small directed acyclic graph:

```text
CUSTOMER_VALIDATED -> ORDER_CREATED -> INVENTORY_RESERVED -> SHIPMENT_SCHEDULED
                              \
                               -> PAYMENT_INITIATED -> ORDER_CONFIRMED
```

Restore verifies dependency completion before an attempt. A successful prerequisite promotes its dependants for re-evaluation. If a prerequisite reaches `EXHAUSTED`, `FAILED_FINAL`, or `FAILED_DEPENDENCY`, every still-blocked descendant transitions to `FAILED_DEPENDENCY` in the same transaction, with an audit entry naming the failed prerequisite. A new event submitted against an already-terminal prerequisite starts in `FAILED_DEPENDENCY`. This fail-fast policy prevents impossible work from remaining blocked forever.

## Circuit breaker: persisted state machine

Every target system owns a circuit breaker. State is stored in the database and guarded against concurrent transitions.

```text
CLOSED --failure threshold--> OPEN --cool-down elapsed--> HALF_OPEN
  ^                                                      |
  +-------------------successful single probe------------+
             failed probe --------------> OPEN
```

- **Closed:** attempts are allowed; consecutive qualifying failures are counted.
- **Open:** ordinary attempts are deferred; the target is given time to recover.
- **Half-open:** exactly one controlled probe is allowed. Success closes the circuit; failure reopens it with a longer cool-down.

This prevents a recovery system from making the outage worse.

## Retry policy and prioritisation

For a transient error, the next eligibility time is calculated with capped exponential backoff and random jitter:

```text
delay = min(maxDelay, baseDelay * 2^attempt) + random(0, jitterWindow)
```

Jitter spreads retries from events that failed at the same time, avoiding a thundering herd.

Among events ready for work, ranking is deterministic and documented:

1. business priority (P1 before P5);
2. approaching business deadline;
3. dependency-unblocking value (number of still-blocked descendants);
4. lower prior attempt count as a final cost-aware tie-breaker;
5. creation time and event ID for stable ordering.

The implementation will explicitly define which fields are ascending/descending and test ties, so the ranking is defensible rather than accidental.

## Downstream simulator and scenarios

A local HTTP simulator plays the role of Salesforce, WMS, or another downstream target. It supports controllable behaviours: success, temporary `503`, rate limit, delayed response, and permanent validation failure. Behavior changes preserve accepted idempotency keys and counters unless a scenario explicitly requests a history reset.

`POST /api/v1/worker/run-once` claims at most ten events with a minimum 30-second visibility lease, then performs each HTTP call outside the claim transaction. The client uses explicit connect/read timeouts, sends the stable event UUID as its downstream idempotency key, classifies `408`, `425`, `429`, and `5xx` responses as transient, and classifies other non-`2xx` responses as permanent. Each attempt records HTTP status when one exists and monotonic request duration.

An integration test covers the ambiguous-outcome case: the client times out, the downstream later commits, and a replay receives duplicate-success without producing a second accepted side effect.

The suite will ship these repeatable scenario controls:

| Scenario | What it proves |
|---|---|
| Dependency cascade | Downstream events remain blocked until their prerequisite succeeds. |
| Thundering herd | 100 temporary failures do not flood a recovering system; circuit and jitter work. |
| Priority and deadline | High-value, time-sensitive work is selected before lower-priority work. |
| Terminal failure | Non-retriable and exhausted events are recorded clearly, without an infinite retry loop. |

The dashboard will display event-status counts, attempt history, circuit state, queue depth, processing rate, success/failure rate, and an event timeline. Data comes from the application’s local database and metrics endpoint, not fabricated screenshots.

## Planned API surface

| Endpoint | Purpose |
|---|---|
| `POST /api/v1/events` | Submit a recovery event. |
| `GET /api/v1/events/{eventId}` | Retrieve current state and attempt history. |
| `GET /api/v1/events?status=...` | Query the operational queue. |
| `POST /api/v1/worker/run-once` | Claim and execute one bounded recovery batch. |
| `PUT /api/v1/simulator/targets/{targetSystem}` | Configure a repeatable downstream behavior. |
| `POST /api/v1/scenarios/dependency-cascade` | Load the dependency demo. |
| `POST /api/v1/scenarios/thundering-herd` | Load the controlled outage demo. |
| `GET /api/v1/circuit-breakers` | Inspect all target-health states. |
| `GET /api/v1/statistics` | Return operational aggregates for the dashboard. |
| `POST /api/v1/admin/events/{eventId}/retry` | Explicit, authenticated operator retry action. |

## Frontend experience and live updates

The Restore dashboard is a Next.js view driven by ordinary REST queries for initial state and Server-Sent Events (SSE) for subsequent updates. SSE is a suitable first choice because the browser needs to receive status changes, while the user does not need a two-way real-time protocol.

The screen has four connected parts:

- a target-system panel showing each circuit as closed, open, or half-open;
- a retry queue table with priority, next attempt time, dependency status, and reason for blocking;
- an event timeline showing state transitions and HTTP outcomes for one correlation ID;
- scenario controls that run the controlled dependency-cascade and thundering-herd demonstrations.

The UI never decides whether an event may retry. It renders the backend’s reason and state. This is important: policy remains testable and authoritative in Java instead of being accidentally duplicated in browser code.

## Technologies and concepts learned

| Area | Technology/concept | Practical learning goal |
|---|---|---|
| State modelling | Java enums, explicit transitions, transition guards | Define legal/illegal state changes and test them as a state machine. |
| Reliable persistence | PostgreSQL, transactions, Flyway | Preserve events, audit history, dependencies, and circuit state across restart. |
| Concurrent workers | `FOR UPDATE SKIP LOCKED`, transaction boundaries | Understand record claiming, duplicate-work risks, and why locks must be short. |
| Resilience | Spring RestClient, timeouts, failure classification, circuit breaker, backoff, jitter | Distinguish transient failures from permanent ones and avoid making an outage worse. |
| API and streaming | Spring REST, OpenAPI, SSE | Design query/mutation endpoints and push state changes to a dashboard. |
| Presentation | Next.js + TypeScript | Build live operational UI without moving business rules into the frontend. |
| Verification | JUnit, Testcontainers, WireMock | Simulate remote errors and verify recovery correctness against a real database. |

## System-design boundaries

Restore is a single durable worker service for version one. It processes a bounded batch rather than loading every due event into memory, and its database is the source of truth. If scaled horizontally later, worker claim locking, idempotency keys, per-target rate limits, and an outbox/event-publishing strategy become central. We will document these as deliberate extension points, while keeping the local project understandable and demonstrable.

## Security, correctness, and operations

- API key authentication protects mutation and administrative operations in the demo environment.
- Request DTOs are validated; payload size is bounded; error responses use a safe, consistent schema.
- Secrets are environment variables and excluded from Git. Sample `.env.example` values are non-sensitive.
- Each state transition and retry attempt has a timestamped audit entry and correlation ID.
- Time is injected/testable rather than read ad hoc throughout algorithm code, enabling deterministic backoff tests.
- HTTP client timeouts, response classification, and idempotency rules are explicit.
- Spring Boot Actuator exposes health and metrics; structured logs support scenario investigation.

## Interview discussion points

1. Why is `BLOCKED` not the same as a failure?
2. Why must retry state survive a process restart?
3. What race condition does `SKIP LOCKED` help avoid, and what does it not solve?
4. How does jitter prevent the thundering-herd problem?
5. Why is a persisted three-state circuit breaker different from a retry counter?
6. How would you make event delivery idempotent when the remote request succeeds but the network response is lost?
7. Why should a final failure be observable and actionable instead of retried forever?

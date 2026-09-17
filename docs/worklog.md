# Faultline worklog

This is a concise, versioned record of shipped engineering decisions. It is intended to help a maintainer or coding agent understand what changed, why it changed, and how it was verified.

## 2026-08-31 — Survey seeded impact analysis

- Shipped a fictional landscape loaded from `survey/src/main/resources/landscape.json` and `POST /api/v1/analysis/impact`.
- Added a framework-independent breadth-first traversal that groups direct and cascade effects into impact rings and remains finite when a reachable cycle exists.
- Verified with focused algorithm and HTTP tests, the full Maven verification suite, and a live local HTTP request.
- Commit: `995c0dc feat(survey): add seeded impact analysis slice`.

## 2026-08-31 — Survey impact priority

- Added an explainable priority report for the seeded outage, including score components, severity band, and deterministic ranking.
- Kept raw reachability and prioritisation as separate API responses so the dashboard can display either view without reconstructing policy in the browser.
- Added algorithm and HTTP contract tests. Verification and commit details will be recorded after this sprint completes.

## 2026-08-31 — Survey active landscape

- Added explicit activation of an immutable, validated landscape import; all graph-reading endpoints now use the same selected topology.
- Preserved the bundled fictional graph as a safe default and added a state-isolated HTTP test for the activation workflow.
- Verified with the full Maven reactor: 21 tests passing.
## 2026-08-31 — Survey metadata-rich landscape

- Extended integrations with explicit data-domain and SLA metadata and expanded the fictional demo to 16 systems and 35 flows.
- Added a reachable legacy cycle that produces a safe recovery prefix plus warning; H2/PostgreSQL-compatible Flyway migration verified by 20 shared/Survey tests.
## 2026-09-01 — Survey persisted impact reports

- Made `POST /api/v1/analysis/impact` create a timestamped immutable incident report and added retrieval by report ID.
- The snapshot contains classification, ranked priorities, recovery output, warnings, and a stable landscape reference; 21 shared/Survey tests verify the workflow and classification rules.
## 2026-09-01 — Survey landscape warnings

- Added a pre-incident cycle-warning endpoint for the active topology and verified the bundled legacy cycle through the 22-test shared/Survey suite.
## 2026-09-01 — Survey dashboard and local runtime

- Added a functional Next.js/TypeScript/D3 dashboard and a three-service Compose topology for PostgreSQL, Survey, and the browser UI.
- Verified frontend type checking and production build, plus the live Compose stack: Survey health is UP; PostgreSQL persists and retrieves impact reports; the dashboard and its internal proxy return 200 with 16 systems and 35 integrations.
- Upgraded the dashboard to Next.js 16.3.4 after npm reported PostCSS advisories; type checking, production build, rebuilt container proxy, and the production dependency audit pass with zero vulnerabilities.

- Reworked the Survey UI into a dark-first operations console with a persisted light-mode choice and a directional D3 topology that focuses the selected dependency neighbourhood; production build and live proxy verification pass.

- Replaced the Survey force graph with a structured four-lane topology board and removed unused D3 packages. The dashboard production build and zero-vulnerability audit pass.

## 2026-09-01 — Restore durable event foundation

- Added persisted recovery events, lifecycle audit records, idempotent submit/retrieve APIs, isolated Flyway history, and the Restore Compose service.
- Added persisted prerequisites: incomplete same-correlation dependencies make a child BLOCKED and participate in idempotency; a later worker slice will release it.
- Added transactional worker claims with persisted visibility leases and expired-lease recovery via `FOR UPDATE SKIP LOCKED`.
- Added owner-guarded completion and atomic dependency release after every prerequisite completes.
- Verified ten focused Restore tests and the full Maven reactor; the rebuilt PostgreSQL container starts with the completion API.

- Added bounded transient retry scheduling and `EXHAUSTED` poison-event handling after three attempts.
- Added persisted three-state target circuits. Open targets are excluded from claims; cooldown admits one half-open probe; success closes the circuit.
- Verified 11 focused Restore tests, the full Maven reactor, Docker migration startup, and a live PostgreSQL claim/failure/circuit-read round-trip.
- Added durable, numbered attempt history for claims, successful completions, transient failures, exhausted retries, and expired worker leases.
- Added the guarded `FAILED_FINAL` transition. Permanent payload failures terminate without retry and do not count against target health.
- Added an HTTP downstream simulator with resettable success, `503`, `429`, delayed-success, and validation-failure behavior plus idempotent success receipts.
- Added bounded real-HTTP recovery execution with explicit timeouts, automatic transient/permanent classification, persisted HTTP attempt evidence, and a tested idempotent replay after an ambiguous timeout.
- Verified the slice with 20 focused Restore tests, the full 42-test reactor, a rebuilt container/Flyway V6, and one isolated live PostgreSQL-backed successful delivery.
- Added fail-fast, transitive `FAILED_DEPENDENCY` propagation so terminal prerequisites cannot strand current or newly submitted dependant events.
- Verified with 21 focused Restore tests, the full 43-test reactor, a rebuilt container, and an isolated live PostgreSQL dependency chain.

## 2026-09-14 — Restore automatic scheduling

- Added opt-in fixed-delay batch execution with validated worker settings, overlap suppression, and failed-tick isolation.
- Proved automatic completion through a real embedded HTTP server and kept the manual worker endpoint on the same executor.
- Verified 27 focused Restore tests, the full 49-test reactor, a rebuilt Java 21 container, and an unchanged 21-event local PostgreSQL snapshot.

## 2026-09-14 — Restore retry jitter

- Added capped exponential backoff with a configurable inclusive jitter window and an injectable production random source.
- Proved policy boundaries and the exact persisted delay with deterministic tests.
- Verified 32 focused Restore tests, the full 54-test reactor, and a live PostgreSQL-backed 5,028 ms retry after one simulated `503`.

## 2026-09-14 — Restore observability and recovery scenarios

- Added zero-filled queue/attempt statistics with nearest-rank p95 receipt-to-success latency.
- Added repeatable dependency-cascade and bounded thundering-herd loaders using production intake rules.
- Added an isolated, assertion-driven benchmark harness. A 50,000-event run completed with 50,010 attempts, 50,000 accepted deliveries, zero pending or terminal events, a 206.45 events/s active drain rate, and a 272.041 s p95 including circuit cool-down.
- Kept the development database intact and removed the benchmark-only containers and volume after verification.

## 2026-09-14 — Restore ordering contract

- Added blocked direct-dependant count to claim ranking and proved priority, deadline, fan-out, and prior-attempt ordering through the HTTP worker path.
- Documented at-least-once delivery with explicit dependency partial order. Independent branches have no FIFO or completion-order guarantee and may run concurrently.

## 2026-09-14 — Restore priority and deadline scenario

- Added a repeatable seven-event scenario whose response declares the expected admission order and exposes each ranking input.
- Proved through the HTTP claim path that five eligible events are ranked independently of creation order while two dependency children remain blocked.

## 2026-09-15 — Restore terminal-failure scenario

- Added deterministic poison-message and retry-exhaustion paths backed by the real downstream simulator and executor.
- Proved bounded attempts, queryable dead-letter states, circuit semantics, and direct plus transitive dependency-failure propagation.

## 2026-09-17 — Restore live updates

- Added a heartbeat-protected SSE endpoint that emits event-state notifications only after their database transaction commits.
- Isolated client delivery behind a bounded single-thread dispatcher; retained notifications stay ordered, while overload may discard older invalidations without delaying durable recovery work.
- Defined snapshot-on-connect and snapshot-on-reconnect semantics so PostgreSQL remains authoritative, and verified the slice with the 66-test reactor plus an isolated Java 21/PostgreSQL lifecycle proof.

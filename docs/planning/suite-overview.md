# Faultline — Integration Resilience Control Plane

> Working name: **Faultline**. The name can be changed before the repository is created.

## 1. The product in one sentence

Faultline is a local-first, enterprise-integration incident simulator and recovery control plane. It determines the business impact of a failed SAP-connected system, recommends a safe recovery order, and then safely reprocesses failed events while respecting dependencies and downstream health.

It is deliberately a **portfolio system**, not a replacement for SAP CPI, Salesforce, or a production incident-management tool. Its value is that it models the operational reasoning a CPI engineer needs during an incident.

## 2. The problem it solves

When an integration endpoint fails, a team typically has two separate problems:

1. **Understanding the blast radius.** Which iFlows, systems, and business processes are affected? Which failures are critical, and which are merely degraded?
2. **Recovering safely.** Which queued messages can retry now? Which must wait for a prerequisite event? Is the downstream target healthy enough to receive traffic?

Most simple retry demos answer neither question. Faultline answers both through two bounded modules.

## 3. The end product

After `docker compose up`, the project will run entirely on a developer machine:

```text
Next.js browser dashboard                 Swagger / OpenAPI
           |                                      |
           +-------- REST + Server-Sent Events ---+
           |                     |
           +--> Survey API --------+----> PostgreSQL
           |       graph analysis  |
           |                       +----> Restore API
           |                             durable recovery worker
           |                                      |
           +--------------------------------------> local downstream simulator
```

The dashboard is a real **Next.js + TypeScript** application, not a static mockup. It has four focused views:

- **Operations overview:** incident count, active alerts, queue depth, processing rate, retry success rate, and per-target circuit state.
- **Landscape health map:** an interactive lane-based topology board of the sample SAP integration estate and its current status.
- **Impact report:** affected systems and iFlows, impact classification, score, cycle warnings, and a recommended restoration sequence.
- **Recovery console:** circuit-breaker state, queue depth, event status counts, processing timeline, and scenario controls.

The topology board is purposeful: its systems and directed paths are the exact data structures returned by Survey, so a viewer can select a failing node and immediately understand why the impact report changes. A structured SVG layout is used because the sample estate is compact, cyclic, and benefits more from stable operational lanes than force simulation; ordinary cards, tables, filters, and status displays remain conventional React components.

The primary deliverables are a reproducible local demo and a live demo on a genuinely free hosting path. The provider will be selected and re-verified near release because free-tier terms change; the local Compose workflow remains the no-cost fallback. The public repository will include a short screen recording/GIF, representative screenshots, API examples, seeded data, automated test results, and architecture documents so a reviewer can assess it without running it.

## 4. Module boundary

| Module | Responsibility | It does not do |
|---|---|---|
| **Survey** | Maintains an integration-landscape graph and produces failure-impact and recovery-order reports. | Send or retry business messages. |
| **Restore** | Stores failed events durably and schedules safe retry attempts based on dependencies, priority, backoff, and circuit health. | Infer the complete enterprise landscape. |

Survey can supply an operator with the recovery context; Restore carries out message-level recovery. They communicate through explicit API contracts and a shared, small domain-model library. Either module can also be demonstrated alone.

## 5. What a reviewer will be able to do

### Scenario A — SAP ECC outage

1. Load a realistic, fictional landscape of 15–20 systems and 35–40 CPI-style integrations.
2. Mark `SAP_ECC` as unavailable.
3. See direct and cascading effects ranked by business impact, such as order synchronisation becoming orphaned and an invoice flow missing its real-time SLA.
4. See an intentional legacy-system cycle reported as a warning rather than causing an infinite traversal.
5. Read a safe recovery sequence generated with Kahn’s topological-sort algorithm.

### Scenario B — dependency-chain recovery

1. Inject an order lifecycle containing dependent events such as customer validation, order creation, inventory reservation, payment, and shipment.
2. Force `ORDER_CREATED` to fail.
3. Observe dependent events become `BLOCKED`, rather than incorrectly retrying and corrupting ordering.
4. Restore the target and watch eligible events release in dependency-safe order.

### Scenario C — downstream thundering herd

1. Inject up to 50,000 events against an isolated local simulator target.
2. Send one bounded batch into repeated `503 Service Unavailable` responses and verify that the open circuit blocks the next claim.
3. Restore the target and observe exactly one half-open probe before parallel bounded recovery resumes.
4. Measure load time, active drain rate, end-to-end recovery time, and p95 receipt-to-success latency while asserting exact attempts and accepted side effects.

## 6. Technical shape

**Language and platform:** Java 21 with Spring Boot 3. This is a deliberate fit for SAP-adjacent enterprise backends: type safety, mature testing, straightforward concurrency primitives, excellent PostgreSQL support, and familiar production conventions.

**Persistence:** PostgreSQL is the source of truth for landscapes, reports, events, retry attempts, and circuit-breaker state. A local in-memory priority queue may optimise work selection, but must never be the only record of a recoverable event.

**Application structure:** one Maven multi-module repository with two Java backend modules plus a presentation application:

```text
faultline/
  survey/                  Java/Spring Boot graph-analysis service
  restore/                 Java/Spring Boot recovery-orchestration service
  shared/                  intentionally small Java contracts/value objects
  dashboard/               Next.js + TypeScript operator UI
  infra/                   Docker Compose and local configuration
  docs/                    designs, ADRs, API examples, learning notes
  test-data/               fictional enterprise landscape and scenarios
```

**Technology choices and rationale:**

| Technology | Where used | Why it belongs here |
|---|---|---|
| Java 21 | Survey and Restore | Strong types, mature concurrency and test ecosystem, natural alignment with enterprise/SAP-adjacent development. |
| Spring Boot 3 | Backend APIs and workers | Production conventions for REST, validation, configuration, metrics, HTTP clients, and dependency injection. |
| PostgreSQL + Flyway | Durable state | Reliable relational modelling, transactions, locking, and reproducible schema history. |
| Next.js + TypeScript | Dashboard | Component-based UI with type-safe API consumption and a conventional React portfolio signal. |
| Structured SVG | Landscape explorer only | Renders and interacts with the graph data using stable operational lanes. |
| Server-Sent Events | Live dashboard updates | A simple one-way server-to-browser channel for retry/circuit state without introducing unnecessary WebSocket complexity. |
| Docker Compose | Local runtime | One reproducible free command starts the whole demonstration. |
| JUnit, Testcontainers, WireMock | Verification | Separates algorithm tests from realistic database and downstream-system tests. |
| GitHub Actions | CI | Ensures public code builds and tests on every push. |

**Quality and security baseline:** schema migrations (Flyway), request validation, problem-details errors, security headers, API-key authentication for protected operations, parameterised persistence through JPA, secrets only in local environment files, audit records, health checks, structured logs, and metrics. There will be no real SAP credentials, personal data, or customer data in the repository.

**Testing:** focused unit tests for algorithms; integration tests with a local PostgreSQL Testcontainers instance; WireMock-based downstream simulations; repeatable scenario tests; and a public GitHub Actions CI workflow for build, test, and static checks.

## 7. Free-only operating model

No paid cloud product is needed to build, test, or demonstrate Faultline.

| Need | Free approach | Cost guardrail |
|---|---|---|
| Source control and public portfolio | GitHub public repository under `shreemangalam` | Do not enable paid add-ons. |
| Local runtime | Java, Gradle, PostgreSQL container, Docker Compose | Everything runs locally. Docker Personal is currently listed at $0 for individual developers. |
| CI | GitHub Actions for a public repository | Keep workflow lean; do not add paid third-party CI. |
| Monitoring demonstration | Spring Boot Actuator plus local Prometheus/Grafana containers, if used | No hosted observability service. |
| API exploration | Swagger UI bundled into the application | No API-management subscription. |
| Demo hosting | Free frontend, backend, and PostgreSQL tiers selected after a release-time terms check; local Compose remains the fallback | No card-required or paid dependency; document sleep/cold-start limits. |

Pricing and terms change, so we will re-check before relying on any hosted feature. The current Docker Personal page lists a $0 individual-developer plan: [Docker pricing](https://www.docker.com/pricing/).

## 8. System-design decisions we will learn

| Concept | In Faultline | Why it matters in an interview |
|---|---|---|
| Service boundaries | Survey analyses the landscape; Restore executes message recovery. | Shows separation of responsibilities and prevents a “god service.” |
| API contracts | Versioned REST request/response schemas and OpenAPI. | Shows that backend changes must protect clients. |
| Data ownership | Each service owns its state; `shared` contains value objects, not shared mutable tables. | Opens a discussion about service coupling and database boundaries. |
| Durable work queues | PostgreSQL persists recovery work and attempt history. | Shows why in-memory queues alone lose work on restart. |
| Concurrency control | Workers claim records transactionally using database locking. | Demonstrates awareness of duplicate processing and race conditions. |
| Eventual consistency | The UI receives periodic/event-driven updates; it does not assume every state changes synchronously. | Mirrors distributed-system reality. |
| Idempotency | Retry requests and downstream calls receive stable identities. | Addresses the “request succeeded but response was lost” failure case. |
| Resilience patterns | Timeouts, failure classification, backoff/jitter, circuit breakers. | Shows recovery logic that avoids amplifying outages. |
| Observability | Correlation IDs, audit records, health endpoints, metrics, and traces in logs. | Lets an operator prove what happened. |
| Threat modelling | Validation, authentication, safe errors, configuration-based secrets. | Shows security is designed in, not appended. |

## 9. End-to-end delivery plan

This is a learning-first plan. A phase ends only when its code, test evidence, documentation, and explanation are understood—not merely when it compiles.

| Phase | Deliverable | Concepts learned | Target effort |
|---|---|---|---|
| 0. Product baseline | Repository, licence, contribution rules, architecture/functional design, ADR template, issue board. | Requirements, scope control, repository hygiene. | 0.5–1 day |
| 1. Platform foundation | Maven modules, Spring Boot skeletons, PostgreSQL, Flyway, Docker Compose, CI, health endpoints. | Dependency management, configuration, migrations, containers, CI. | 1–2 days |
| 2. Survey core | Graph model, import validation, BFS impact analysis, cycle detection, unit tests. | Directed graphs, adjacency lists, BFS, defensive input handling. | 2–3 days |
| 3. Survey product slice | Impact scoring, Kahn recovery sequence, persisted reports, OpenAPI, sample landscape. | Topological sort, scoring trade-offs, API and persistence design. | 1–2 days |
| 4. Dashboard and demo | Next.js shell, structured landscape explorer, impact report UX, API client, screenshots. | React components, TypeScript contracts, accessible data visualisation, and interaction design. | 2–3 days |
| 5. Restore core | Event state machine, dependencies, durable scheduler, attempt audit, downstream simulator. | State machines, transactions, scheduling, idempotency boundaries. | 2–3 days |
| 6. Resilience behaviour | Circuit breaker, backoff with jitter, deterministic prioritisation, concurrency tests. | Failure modes, retry storms, locking, ranking and invariants. | 2–3 days |
| 7. Integration and live UI | SSE updates, scenario controls, metrics/statistics, Help / How It Works, full workflow from outage to recovery. | Event-driven UI, observability, service integration. | 1–2 days |
| 8. Release and interview readiness | Hardening, test report, demo recording, README, two project case studies, file-by-file review. | Release discipline, technical storytelling, interview communication. | 2–4 days |

At 2–3 focused hours per day, this is approximately **3–4 weeks**. An intensive build can produce the functional suite in about two weeks, but the final review and learning sessions are important enough that they should not be compressed away.

The first visible milestone lands early: after Phases 1–4, Survey is already a complete, polished project with a real interactive frontend. Restore then becomes the second project/case study while still benefiting from shared product context.

## 10. Two-project resume packaging

One GitHub repository is a sensible engineering choice; two self-contained case studies are a sensible resume choice.

| Resume project | Focus | Evidence to link/show |
|---|---|---|
| **Faultline Survey — Integration Landscape Impact Analyzer** | Graph algorithms, SAP-CPI domain modelling, REST API design, interactive topology visualisation. | Impact-demo GIF, graph screen, sample report, algorithm tests. |
| **Faultline Restore — Dependency-Aware Event Recovery Orchestrator** | Durable event processing, database concurrency, circuit breaker, backoff/jitter, operational metrics. | Thundering-herd video, state timeline, circuit view, integration tests. |

Each case study will state that it is part of Faultline and link to its module. This is transparent: it presents two substantial problem domains without pretending that one codebase is two unrelated repositories.

## 11. What makes it resume-worthy

The resume claim will be precise and evidence-backed, rather than inflated. Example wording after completion:

> Built **Faultline**, a Java/Spring Boot integration-resilience control plane for SAP-style landscapes. Implemented graph-based impact analysis (BFS and Kahn’s topological sort), durable dependency-aware event recovery, persisted circuit breakers, and jittered exponential backoff; shipped containerised local demos, automated integration tests, and operational metrics.

Potential supporting bullet, only if measured by the final demo:

> Demonstrated controlled recovery of a 100-event simulated outage, preserving dependency ordering and preventing retry storms through a persisted circuit-breaker state machine.

This lets you credibly discuss distributed-systems failure modes, algorithms, database concurrency, API design, testing, and observability—not merely SAP configuration.

## 12. Scope discipline

The first version will not attempt to integrate with a real CPI tenant, SAP system, Salesforce instance, Kafka cluster, or third-party alerting tool. Those would introduce credentials, cost, and setup friction without improving the core demonstration. Instead, protocol-aware local simulators and realistic fictional data make the concepts observable and repeatable.

We will first complete an end-to-end vertical slice: a seeded landscape, one impact report, one recovered event, automated tests, and a documented local demo. Advanced dashboard polish and optional integrations come only after this slice is solid.

## 13. Learning and review contract

Every implementation sprint will end with:

1. An updated functional/technical design section and any relevant ADR.
2. A small set of test evidence and a runnable demo command.
3. A plain-English learning note explaining the concepts introduced.
4. Interview questions tied to the exact code written.

At the end, we will conduct a file-by-file review. The goal is that you can explain the responsibility, inputs/outputs, error behaviour, test rationale, and trade-offs of each non-trivial file in your own words.

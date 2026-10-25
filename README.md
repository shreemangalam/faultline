# Faultline

**An integration resilience control plane for SAP-style enterprise landscapes.**

Faultline is a portfolio-grade, local-first system that answers two operational questions during an integration failure:

1. What business systems and CPI-style flows are affected, and what should recover first?
2. How can failed events be replayed safely without violating dependencies or overwhelming an unhealthy target?

It contains two complementary backend modules and one operator dashboard. The operator connects Survey's landscape assessment to Restore's recovery work; creating a report does not automatically enqueue messages.

| Component | Responsibility |
|---|---|
| `survey` | Models the landscape as a directed graph and produces impact/recovery reports. |
| `restore` | Performs durable, dependency-aware retry orchestration. |
| `dashboard` | A Next.js operational view of the backend’s real data and scenarios. |

## Status

Survey provides graph-based impact analysis, immutable landscape imports, and persisted incident reports. Restore provides durable event intake, dependency-aware recovery, worker leases, bounded retries with jitter, persisted target circuits, attempt/audit evidence, a controllable HTTP simulator, opt-in scheduling, and live updates. The dashboard includes Survey, Restore, and an operator guide in light and dark themes. See the [architecture](docs/architecture.md) for component boundaries and the recovery lifecycle.

No real SAP, CPI, Salesforce, customer data, or credentials are used. All demo systems and payloads are fictional and run locally.

## Documentation

- [Documentation index](docs/README.md)
- [Architecture](docs/architecture.md)
- [Operations and verification](docs/operations.md)
- [Restore 50,000-event benchmark](docs/technical/restore-recovery-benchmark.md)
- [Restore delivery and ordering contract](docs/technical/restore-delivery-ordering.md)
- [Restore terminal-failure contract](docs/technical/restore-terminal-failures.md)
- [Restore live-update contract](docs/technical/restore-live-updates.md)
- [Restore circuit probes and crash recovery](docs/technical/restore-circuit-probes.md)
- [API security and downstream credentials](docs/technical/api-security.md)

## Implemented stack

Java 21+, Spring Boot, Maven, PostgreSQL, Flyway, Next.js, TypeScript, Docker Compose, JUnit, PostgreSQL Testcontainers, Playwright, and GitHub Actions. See [development and verification instructions](CONTRIBUTING.md).

## Run Faultline locally

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
# Choose a local FAULTLINE_API_KEY in .env before starting.
docker compose --env-file .env -f infra/compose.yaml up --build
```

Open the operator dashboard at `http://localhost:3000`. Survey listens on `http://localhost:8081` ([contract](docs/api/survey-openapi.json)); Restore listens on `http://localhost:8082` ([contract](docs/api/restore-openapi.json)). Both expose JSON at `/v3/api-docs`. Supplied Compose ports bind to localhost. Direct API mutations require `X-Faultline-Api-Key`; the dashboard injects it on the server. The dashboard has no user login and is intended for local use. Automatic Restore delivery is disabled by default; set `RESTORE_WORKER_ENABLED=true` only when that process should execute eligible queue data.

Run the isolated recovery benchmark with `./scripts/benchmark-restore.ps1 -Port 19083` when the operations demo already occupies 18082. It defaults to 50,000 events and removes only its dedicated containers and database volume after verification. Set the shell's `FAULTLINE_API_KEY` when selecting a non-default benchmark credential; the benchmark passes it to its service and HTTP clients.

## Curated operations walkthrough

Use the [isolated operations demo](docs/operations.md) for the 39-message Salesforce outage narrative on port 3002. It has a separate PostgreSQL volume and keeps the existing development database intact. Automatic delivery remains disabled; recovery is manual and bounded.

## Operations console

The dashboard focuses on incident exposure and recoverable business work, with a topology inspector, backend-ranked recovery assessment, counted queue filters, message-level evidence and separately confirmed demo tools. The curated scenario uses real HTTP execution and PostgreSQL evidence rather than fabricated metrics.

![Restore operations console](docs/evidence/2026-10-18/restore-before.png)

Watch the [recorded Survey-to-recovery walkthrough](docs/evidence/2026-10-18/faultline-walkthrough.webm), then inspect the [verified final state](docs/evidence/2026-10-18/restore-after.png).

See [operations and verification](docs/operations.md) for browser checks and data-preservation instructions.

## Version-one boundaries

Survey performs a declared complete-outage analysis; its healthy topology is not live remote telemetry. Restore executes a fictional HTTP simulator contract and retains terminal failures for investigation; terminal-message redrive is deferred. Delivery is at least once with explicit dependency ordering, and simulator deduplication is process-local. Real SAP/CPI connectivity, multi-user authentication, and hosted production operation are separate scopes. Historical benchmark results describe their recorded revision and environment, not a production capacity guarantee.

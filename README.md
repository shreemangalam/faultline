# Faultline

**An integration resilience control plane for SAP-style enterprise landscapes.**

Faultline is a portfolio-grade, local-first system that answers two operational questions during an integration failure:

1. What business systems and CPI-style flows are affected, and what should recover first?
2. How can failed events be replayed safely without violating dependencies or overwhelming an unhealthy target?

It contains two linked backend modules and one operator dashboard:

| Component | Responsibility |
|---|---|
| `survey` | Models the landscape as a directed graph and produces impact/recovery reports. |
| `restore` | Performs durable, dependency-aware retry orchestration. |
| `dashboard` | A Next.js operational view of the backend’s real data and scenarios. |

## Status

Survey is a complete, containerised impact-analysis module. Restore now has durable event intake, dependency-aware release, worker leases, bounded retries, and persisted target circuits; downstream execution and observability are next. The public design baseline is available in [`docs/planning`](docs/planning).

No real SAP, CPI, Salesforce, customer data, or credentials are used. All demo systems and payloads are fictional and run locally.

## Public design documents

- [Suite overview and delivery plan](docs/planning/suite-overview.md)
- [Survey module design](docs/planning/survey-module.md)
- [Restore module design](docs/planning/restore-module.md)

## Local development policy

The repository will contain production-style source, tests, architecture/design material, sample data, and public demo evidence. Personal interview preparation, detailed learning notes, and scratch material live in the Git-ignored `private/` directory so they are not published accidentally.

## Planned stack

Java 21 or later LTS, Spring Boot, Maven, PostgreSQL, Flyway, Next.js, TypeScript, Docker Compose, JUnit, Testcontainers, WireMock, and GitHub Actions.

## Run Faultline locally

```powershell
docker compose -f infra/compose.yaml up --build
```

Open the operator dashboard at `http://localhost:3000`. The Survey API listens on `http://localhost:8081`; its versioned contract is [here](docs/api/survey-openapi.json). Restore listens on `http://localhost:8082` and persists recovery events locally.
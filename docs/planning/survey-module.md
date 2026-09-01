# Faultline Survey — Integration Landscape Analysis

> Working module name: **Survey**. It replaces the initial “Hermes” concept.

## Purpose

Survey models an enterprise integration landscape as a directed graph and produces an operational report when a system degrades or fails. It answers: **what is affected, how severe is it, and what should be restored first?**

The module is designed around recognizable SAP CPI patterns: SAP ECC/S/4HANA sources, CPI iFlows, REST/SOAP partners, a Salesforce target, a WMS, file-transfer endpoints, and downstream portals. All systems and data are fictional.

## Inputs and outputs

### Inputs

- A landscape definition: systems, integration edges/iFlows, protocol, data domain, SLA class, criticality, and alternate-source relationships.
- An incident declaration, for example `{ "systemId": "SAP_ECC", "degradationType": "COMPLETE_OUTAGE" }`.

### Output: an impact report

The report is a persisted, time-stamped artifact containing:

- affected systems and integrations grouped into direct and cascade impact rings;
- an `ORPHANED`, `PARTIAL`, or `DEGRADED` classification;
- an explainable impact score and severity band;
- a recovery sequence for the affected acyclic subgraph;
- warnings for cycles or invalid landscape configuration.

## Core model

```text
System node --[Integration edge / iFlow metadata]--> System node

SAP_ECC --RFC/order master--> CPI_ORDER_SYNC --REST/order--> SALESFORCE
                         \--SOAP/shipment--> LEGACY_WMS
```

The runtime graph uses an adjacency list, conceptually `Map<SystemId, List<IntegrationEdge>>`. The project will implement this structure and traversal logic itself rather than use a graph library. That keeps the algorithm and metadata model visible for learning and interviews.

The persisted model separates stable landscape configuration from changing operational state. A system’s business criticality is configuration; its current health is operational state. This distinction prevents an incident update from rewriting the architecture record.

## Algorithms to implement and explain

### Breadth-first search (BFS) for impact analysis

Starting from a degraded node, Survey traverses outgoing edges. A visited set prevents revisiting a system. The BFS distance gives a natural impact ring:

- one hop: direct impact;
- two or more hops: cascade impact.

BFS is chosen because it produces the operationally intuitive “nearest effects first” view. DFS could find reachability too, but would not naturally order the report into impact rings.

### Consumer classification

For an affected target, Survey inspects its incoming, healthy data sources:

- **Orphaned:** the failed path was the target’s only viable input.
- **Partial:** another viable source remains, but capacity/data coverage is reduced.
- **Degraded:** the target is reachable but an integration or SLA is affected.

These are business-impact labels; they must be derived from explicit landscape metadata, not guessed.

### Explainable scoring

The first version will use a documented weighted formula derived from:

- configured business criticality (1–10);
- computed fan-out of the affected node;
- SLA class (real-time, near-real-time, batch);
- impact distance and orphaned status.

Every report includes the score components, so an operator can challenge or tune the weighting. A score should guide priority, not pretend to be an objective truth.

### Kahn’s algorithm for recovery order

For the affected subgraph, Survey builds incoming-dependency counts. Nodes with zero remaining prerequisites are emitted first; then their outgoing edges reduce the next nodes’ counts. The result is a dependency-safe recovery order.

If nodes remain after the queue empties, the subgraph contains a cycle. Survey reports the cycle as a warning and does not invent an unsafe automatic order.

## Planned API surface

| Endpoint | Purpose |
|---|---|
| `POST /api/v1/landscape/systems` | Register or update a system node. |
| `POST /api/v1/landscape/integrations` | Register an integration edge/iFlow. |
| `POST /api/v1/landscape/import` | Load validated sample JSON. |
| `POST /api/v1/analysis/impact` | Create an impact report for an incident. |
| `GET /api/v1/analysis/impact/{reportId}` | Retrieve the persisted report. |
| `POST /api/v1/analysis/recovery-sequence` | Generate a recovery plan. |
| `GET /api/v1/landscape/health-map` | Return graph and current health for the UI. |
| `GET /api/v1/landscape/warnings` | List detected anomalies and cycles. |

The exact schema will be versioned in OpenAPI before the implementation is considered complete.

## Frontend experience and API contract

Survey exposes a purpose-built health-map response for the Next.js dashboard. It returns node IDs, labels, health, criticality, and edge metadata—data that the topology board uses directly to render and focus the landscape graph. It must not expose a database entity shape just because that is convenient for Java.

The user journey is deliberate:

1. The operator selects a failed system in the topology board or chooses a preset outage.
2. The dashboard posts an incident declaration to Survey.
3. Survey stores and returns the report ID.
4. The report page presents impact rings, score explanation, cycle warnings, and recovery order beside the highlighted graph path.

This introduces a useful frontend/backend boundary: the Java backend owns graph reasoning; Next.js owns interaction, rendering, loading/error states, and accessible presentation. TypeScript interfaces will be generated or manually validated from the OpenAPI contract to prevent silent API drift.

## Technologies and concepts learned

| Area | Technology/concept | Practical learning goal |
|---|---|---|
| Domain modelling | Java records, enums, value objects | Represent systems, iFlows, protocols, SLAs, and incident status without primitive-string confusion. |
| Algorithm design | Adjacency list, BFS, visited sets, Kahn’s algorithm | Explain the time/space trade-off and prove cycles do not break analysis. |
| API design | Spring MVC, DTOs, Bean Validation, OpenAPI | Separate public contracts from persistence and produce useful client errors. |
| Data modelling | PostgreSQL, JPA, Flyway | Model nodes, edges, reports, and health snapshots with controlled migrations. |
| Visualisation | Next.js, TypeScript, structured SVG layout | Render a meaningful graph with stable operational lanes and support selection/keyboard-readable alternatives. |
| Testing | JUnit, Testcontainers, Playwright later | Test algorithms, APIs, database mappings, and the graph/report user journey. |

## System-design boundaries

Survey starts as a single service because the sample landscape is intentionally small and a single process gives deterministic, fast analysis. If it later needed to analyse many tenants or massive graphs, the design discussion would include caching immutable topology, asynchronous report jobs, tenant isolation, and graph-database evaluation. Those are future-scale considerations, not excuses to over-engineer version one.

## Demo data and visual proof

The repository will contain a readable `landscape.json` with 15–20 systems and 35–40 meaningful edges. It will include:

- order-to-cash, procure-to-pay, inventory, and invoice flows;
- protocol examples: RFC, IDoc-style payload metadata, REST, SOAP, SFTP/file;
- junction systems and alternate source paths;
- one deliberate legacy cycle, clearly labelled as test data.

The main demo calls the incident endpoint for SAP ECC. The UI and JSON report show affected processes, ranked priorities, and recovery steps. Screenshot and API-response fixtures will be committed as evidence.

## Quality rules

- Validate IDs, relationships, duplicate edges, and invalid criticality values before graph construction.
- Treat unknown systems and malformed imports as explicit client errors.
- Never recurse without cycle protection.
- Keep algorithm classes independent of Spring, making unit tests fast and illustrative.
- Use integration tests for persistence and API contracts, and unit tests for each algorithmic case.

## Interview discussion points

1. Why does an integration landscape map naturally to a directed graph?
2. Why is BFS preferable to DFS for the impact-report user experience?
3. How does a visited set prevent an infinite traversal?
4. Why use Kahn’s topological sort instead of a recursive DFS ordering?
5. Why must impact scoring be explainable and configurable?
6. What changes would be required to analyse a landscape too large to keep in one process?

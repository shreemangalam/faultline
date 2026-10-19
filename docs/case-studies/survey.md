# Faultline Survey: explainable integration impact analysis

## Problem

An unavailable enterprise system can affect many integrations indirectly. An operator needs to distinguish direct exposure from cascades, understand business priority, and identify dependencies that prevent a complete restoration order.

## Implementation

Survey models a fictional SAP-style estate as a directed graph: 16 systems and 35 integrations, including protocol, data-domain, SLA, and business-criticality metadata. Java domain classes implement breadth-first impact traversal, consumer classification, explainable scoring, and Kahn's topological recovery planning. Cycle protection guarantees finite traversal; unresolved nodes remain visible rather than receiving an invented safe order.

Operators can validate and store immutable landscape imports, explicitly activate one, and create a persisted incident report. The report retains its topology reference and complete result so a later configuration change cannot rewrite historical evidence. Spring Boot exposes versioned APIs; PostgreSQL/Flyway owns durable configuration and reports. A Next.js dashboard supports direct-connection tracing, a full-landscape view, report inspection, themes, and keyboard interaction.

## A concrete demonstration

1. Select `SAP_ECC` and analyze a complete outage.
2. Follow `CPI_ORDER_SYNC` to downstream consumers and explain the impact rings.
3. Inspect a score's criticality, fan-out, SLA, and proximity components.
4. Explain the intentional WMS/inventory cycle and the unresolved recovery remainder.
5. Retrieve the report by ID, then explain why activating another topology would not change it.

## Trade-offs and boundaries

The compact graph is analyzed synchronously in memory; relational storage preserves configuration/history. Classification is a rule-based summary, not measured fallback capacity. The healthy topology map is not continuous endpoint telemetry. Only complete-outage analysis is supported. Integration protocols are metadata, not implemented SAP/RFC/SOAP/SFTP adapters. A Survey report does not automatically create Restore events.

## Evidence and learning

The [release verification](../worklog/2026-10-18-release-readiness.md) includes 23 passing Survey tests and real browser verification of all 16 system neighborhoods across four sizes and both themes. See the [API contract](../api/survey-openapi.json), [priority rules](../technical/survey-impact-priority.md), [recovery algorithm](../technical/survey-recovery-sequence.md), and [learning path](../learning-path.md).

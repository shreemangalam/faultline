# Survey landscape health map

`GET /api/v1/landscape/health-map` returns a dashboard-specific view of the seeded graph: system identifiers, labels, criticality, health, and integration metadata. It avoids exposing Java-domain objects directly to a future D3 client.

The current slice reports the seeded baseline, so every system is `HEALTHY`. An impact analysis is read-only and does not mutate operational health. Persisted health snapshots and incident-driven updates are a later capability; this response shape is stable groundwork for that change.

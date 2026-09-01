# Survey persisted impact reports

`POST /api/v1/analysis/impact` creates a timestamped report and returns `201 Created` with `Location: /api/v1/analysis/impact/{reportId}`. `GET` on that location returns the immutable report snapshot.

A report stores its incident, impact rings, data-domain/SLA edge metadata, consumer classifications, priority score explanation, recovery plan, and warnings as one JSON snapshot. This makes a past operator result stable even when a different landscape is activated later. The record also retains a landscape reference: either the active import ID or `BUNDLED_SAMPLE`.

Consumer classification is evidence-based. An affected target is `PARTIAL` only when an unaffected source supplies the same data domain; it is `ORPHANED` when every incoming feed is affected; otherwise it is `DEGRADED`.
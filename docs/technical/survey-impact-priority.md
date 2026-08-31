# Survey impact priority

`POST /api/v1/analysis/impact-priorities` ranks the systems reached by the seeded-landscape impact analysis. The endpoint is intentionally separate from the raw impact report so consumers can choose graph detail or an operator-facing priority list.

For each affected system, the score is the sum of four visible components:

- criticality: configured criticality (1–10) multiplied by 10;
- fan-out: two points per direct downstream integration, capped at 20;
- SLA: 20 for RFC, REST, or SOAP; 10 for IDoc; 0 for other protocols in this seed data;
- proximity: 20 for a direct impact, reduced by five for each additional ring, with a floor of zero.

Scores map to `CRITICAL` (120+), `HIGH` (90–119), `MODERATE` (60–89), and `LOW` (below 60). Results are ordered by descending score and system ID, making equivalent analyses deterministic.

The score is a prioritisation aid, not an automated recovery decision. Consumer classification and alternate-source capacity are not yet modelled, so orphaned status is deliberately absent from this formula.

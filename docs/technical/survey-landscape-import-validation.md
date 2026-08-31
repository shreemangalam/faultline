# Survey landscape import validation

`POST /api/v1/landscape/import/validate` checks a candidate graph without changing the active seeded landscape. A valid candidate returns `200`; an invalid candidate returns `400` with all detected field issues in one response.

The validator checks system and integration identifier formats, duplicate IDs, blank labels/protocols, criticality range, and integration source/target relationships. It is deliberately framework-independent, so the same rules can guard a future persisted import workflow.

This endpoint is a validation preview, not the planned state-changing import. Saving a validated landscape becomes safe only after topology and operational state have a durable owner.

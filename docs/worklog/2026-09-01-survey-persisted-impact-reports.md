# Survey persisted impact reports

- Changed the impact endpoint into report creation (`201 Created`) and added report retrieval by ID.
- Persisted a complete, immutable report snapshot with its topology reference, classifications, priority explanation, recovery plan, and cycle warning.
- Added focused tests for report retrieval and every consumer-impact classification; verified with 21 shared/Survey tests.
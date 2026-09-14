# Restore thundering-herd scenario

- Added `POST /api/v1/scenarios/thundering-herd` with a 100-event default and a validated 1–50,000 event range.
- Each run atomically persists an isolated eligible queue and two audit entries per event in 500-record Hibernate batches, then places its unique simulator target in temporary outage.
- Extracted request fingerprinting so bulk-loaded events use the same canonical identity calculation as normal intake.
- The HTTP integration test proves that a ten-event batch may finish after the circuit threshold is crossed, but the next claim is empty: outage traffic overshoot is bounded by the configured batch size.
- Verified 38 focused Restore tests and the full 60-test Maven reactor. The rebuilt Java 21 service loaded 100 PostgreSQL events plus 200 audits in 323 ms with zero deliveries, preserved the prior 28 events, and left Survey, Restore, and the dashboard healthy.

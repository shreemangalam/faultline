# Restore dependency-cascade scenario

- Added `POST /api/v1/scenarios/dependency-cascade` to create an isolated six-event order-recovery graph through the normal durable intake path.
- Each run receives unique correlation, idempotency, and target identifiers so prior circuit or simulator state cannot make the demonstration nondeterministic.
- Scenario creation is one transaction and returns named steps, initial states, targets, and prerequisite IDs for UI rendering.
- The HTTP integration test executes the complete graph in bounded batches of 1, 1, 2, 1, and 1, then verifies six completed events and six successful attempts through the statistics endpoint.
- Verified 36 focused Restore tests and the full 58-test Maven reactor. The rebuilt Java 21 service loaded a live PostgreSQL scenario with one eligible and five blocked events, six dependency edges, and twelve audit rows; the prior 22 events were preserved and all three application surfaces remained healthy.

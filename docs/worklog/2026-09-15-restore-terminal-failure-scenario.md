# Restore terminal-failure scenario

- Added `POST /api/v1/scenarios/terminal-failures` with isolated permanent-rejection and retry-exhaustion paths.
- The fixture declares downstream behavior, expected terminal state, and expected attempt count, and includes direct and transitive blocked descendants.
- The end-to-end test drives real simulated HTTP responses through the production executor and verifies event history, terminal-state queries, aggregate statistics, circuits, simulator request counts, and dependency failure propagation.
- Verification passed with three focused scenario tests, the full 63-test reactor, and an isolated Java 21/PostgreSQL run using natural 5s and 10s retry delays. The temporary stack and volume were removed; the development queue remained at 135 records.

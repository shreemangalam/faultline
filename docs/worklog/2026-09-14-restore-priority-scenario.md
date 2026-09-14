# Restore priority and deadline scenario

- Added `POST /api/v1/scenarios/priority-deadline`, which loads seven isolated events in an intentionally non-ranked creation order.
- The response declares the expected order and exposes priority, deadline, state, target, and prerequisites for each event.
- The HTTP integration test claims the five eligible events and proves that priority, deadline, and blocked-dependant fan-out control admission while two child events remain blocked.
- Verification passed with two focused scenario tests and the full 62-test reactor. A rebuilt Java 21 service backed by PostgreSQL returned five eligible and two blocked events while preserving the existing 128 records, bringing the development total to 135.

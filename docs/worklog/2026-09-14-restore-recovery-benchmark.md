# Restore recovery benchmark

- Added an isolated PostgreSQL/Restore Compose environment and an assertion-driven PowerShell harness for 3–50,000 events.
- The harness proves circuit containment, half-open recovery, concurrent bounded claims, exact attempt accounting, idempotent accepted deliveries, and final queue/circuit health.
- Measured 50,000 events with eight workers: 7.916 s load, 242.190 s active drain at 206.45 events/s, and 272.041 s p95 receipt-to-success latency including the circuit cool-down.
- Verified 50,010 attempts, 50,000 accepted deliveries, zero pending events, zero terminal failures, and a final `CLOSED` circuit; the isolated database volume was removed after the run.

# Project documentation

| Document | Purpose |
|---|---|
| [Architecture](architecture.md) | Components, data ownership, analysis rules, and recovery lifecycle |
| [Operations](operations.md) | Start the isolated demo, execute bounded recovery, and run verification |
| [API security](technical/api-security.md) | Operator credentials, proxy boundary, and downstream credentials |
| [Survey API](api/survey-openapi.json), [Restore API](api/restore-openapi.json) | Versioned request and response contracts |
| [Delivery and ordering](technical/restore-delivery-ordering.md) | At-least-once delivery, prerequisite ordering, and claim ranking |
| [Circuit probes](technical/restore-circuit-probes.md) | Target admission, probe ownership, and crash recovery |
| [Terminal failures](technical/restore-terminal-failures.md) | Permanent rejection, retry exhaustion, and dependency failure |
| [Live updates](technical/restore-live-updates.md) | SSE delivery and snapshot/reconnection semantics |
| [Recovery benchmark](technical/restore-recovery-benchmark.md) | Reproduction procedure, measured result, and limitations |

The repository documentation describes the shipped application and how to operate it.

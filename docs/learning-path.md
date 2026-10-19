# Relearn Faultline

Start with one user journey, then trace its code across layers. Plans explain the original intent; technical contracts describe current behavior; dated worklogs explain decisions and historical verification.

| Step | Learn | Read | Exercise |
|---|---|---|---|
| 1 | Why the application exists | [README](../README.md), [suite overview](planning/suite-overview.md) | Explain impact analysis versus message recovery |
| 2 | Graph reasoning | [Survey design](planning/survey-module.md), [priority scoring](technical/survey-impact-priority.md), [recovery order](technical/survey-recovery-sequence.md) | Trace BFS rings and Kahn's algorithm on a small graph with a cycle |
| 3 | Configuration and history | [import validation](technical/survey-landscape-import-validation.md), [persistence](technical/survey-landscape-persistence.md), [activation](technical/survey-active-landscape.md), [reports](technical/survey-persisted-impact-reports.md) | Explain save versus activate and why a report remains unchanged |
| 4 | Durable work | [Restore design](planning/restore-module.md), [event foundation](technical/restore-event-foundation.md), [ordering](technical/restore-delivery-ordering.md) | Follow intake, claim, HTTP execution, and completion in source |
| 5 | Failure handling | [terminal failures](technical/restore-terminal-failures.md), [circuit probes](technical/restore-circuit-probes.md), [API security](technical/api-security.md) | Explain a timeout, a 503, and a permanent rejection |
| 6 | UI and evidence | [live updates](technical/restore-live-updates.md), [operations demo](worklog/2026-09-17-operations-demo.md), [console QA](worklog/2026-09-18-console-qa.md) | Observe dependency release and reconnect-driven REST refresh |
| 7 | Measurement and trade-offs | [benchmark](technical/restore-recovery-benchmark.md), [worklog index](worklog.md) | State what the benchmark measured and what it cannot establish |

For a request trace, follow controller → request DTO → service/transaction → domain rule or repository → response DTO → UI. Tests show the edge cases the implementation promises. For each nontrivial class, identify its responsibility, inputs, outputs, changed state, failure behavior, transaction boundary, and associated test.

Older notes can predate shipped functionality: import validation preceded immutable persistence; D3 was replaced by a structured SVG graph; direct mutation examples now require an API key. Current controllers and technical contracts take precedence over those historical examples.

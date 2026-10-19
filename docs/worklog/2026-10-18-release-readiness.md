# Local release readiness — 18 October 2026

Completed a release pass over the existing Survey/Restore/dashboard implementation. This is evidence for a local portfolio release; it is not production certification or proof of a remote CI run.

## Changes

- Corrected the unknown-system test's mismatched request/expected identifier.
- Authenticated Restore's built-in simulator HTTP calls. A separately configured downstream receives only an explicitly configured target credential, not the local operator key implicitly. Updated the recovery benchmark's client/service credential wiring.
- Added migration V7 for persisted half-open probe identity and lease expiry. Abandoned probes can be replaced; permanent probe rejection closes target admission while preserving terminal event evidence; unrelated late results cannot settle another event's probe.
- Repaired a clean-start race found during real container verification: Survey can now initialize its migration history at version zero when Restore has already populated the shared schema. A regression loads the deployment configuration against pre-existing table data and proves all four Survey migrations apply without changing that data.
- Exported both runtime OpenAPI contracts, including mutation security. Snapshot comparisons omit deployment-specific server addresses.
- Upgraded Next.js from 16.3.4 to 16.3.8 and applied compatible transitive dependency fixes. The production dependency audit reports zero known vulnerabilities at verification time. Upstream advisories include [the ImageResponse issue](https://github.com/vercel/next.js/security/advisories/GHSA-vcvr-r3jv-pc5j) and [image-optimization SSRF](https://github.com/vercel/next.js/security/advisories/GHSA-cjq9-62q9-8jv4); package audit severity alone does not establish that this application exposes every affected feature.
- Bound supplied Compose ports to loopback and made operations host ports configurable. Excluded local environment files and temporary material from image build contexts.
- Updated setup/design/technical guidance, added a [learning path](../learning-path.md), [contribution/setup instructions](../../CONTRIBUTING.md), and separate [Survey](../case-studies/survey.md) and [Restore](../case-studies/restore.md) case studies. Browser/recording evidence paths now resolve relative to the repository, including when invoked through npm from `dashboard/`.

## Verification

| Check | Result |
|---|---|
| Full Maven reactor, PostgreSQL Testcontainers | **81 tests passed**: Shared 2, Survey 23, Restore 56; zero failures/errors/skips |
| Security-enabled real HTTP delivery | Success completes; 503 schedules retry; 422 stops permanently; unauthenticated simulator mutation returns 401 |
| Downstream credential isolation | A configured target receives no implicit local operator credential; explicit target credential is honored |
| Circuit recovery | Exclusive probe, expired-probe replacement, stale-worker refusal, permanent-probe settlement, unrelated-late-result isolation |
| Fresh/shared-schema initialization | Reproduced Restore-first failure, repaired startup, passed deployment-config regression and real container startup |
| Dashboard type check and production build | Passed with Next.js 16.3.8 |
| Production npm audit | Zero known vulnerabilities reported |
| Compose models | Development, operations, and benchmark configurations validate |
| Container build | Java 21 backend images and production dashboard built and ran locally |
| API snapshots/security boundary | 12 Survey paths, 19 Restore paths; public reads, protected direct mutations, safe errors, headers, server-side proxy passed |
| Browser system checks | Both themes at 1440×900, 1366×768, 768×1024, 390×844; no page errors; graph paths, keyboard navigation, filters, inspector, loading/error states, and SSE reconnect pass |
| Authenticated recovery smoke benchmark | 100 events, 110 attempts, 100 accepted deliveries, zero pending/terminal work, CLOSED final circuit; [result](../evidence/restore-smoke-2026-10-18.json) |
| Recorded UI recovery | Seven batches: 24 pending → zero; 13 completed → 37; two terminal failures retained; second tab updates through SSE without manual refresh |

Backend verification ran locally on JDK 25 with a Java 21 compilation target; packaged services ran on Java 21. Browser verification used Playwright 1.62.1 with its matching downloaded Chromium runtime. The earlier installed browser was incompatible, so it was not used for the passing run. The historical [50,000-event result](../technical/restore-recovery-benchmark.md) was not rerun or relabeled as a new measurement.

## Evidence and reproduction

- [Recorded walkthrough](../evidence/2026-10-18/faultline-walkthrough.webm)
- [Survey outage assessment](../evidence/2026-10-18/survey-incident.png)
- [Restore before recovery](../evidence/2026-10-18/restore-before.png)
- [Restore after recovery](../evidence/2026-10-18/restore-after.png)
- [Retained terminal evidence](../evidence/2026-10-18/terminal-evidence.png)

The main verification environment uses Compose project `faultline-release-check`, dashboard port 3003, API ports 19081/19082, and a new dedicated operations volume. Recording used a separate `faultline-release-record` volume and the same built application images on ports 3004/19091/19092. Neither environment mounts the pre-existing demo/development volumes. The benchmark's dedicated containers and volume were removed by its harness after assertions passed. Existing demo records were not reset or rewritten.

Final read-only comparison: the existing port-3002 demo retains 39 events, 24 pending, 13 completed, two terminal failures and 19 attempts. The updated port-3003 verification demo has 39 events, zero pending, 37 completed, two terminal failures and 42 attempts. The extra recording containers were stopped after capture; their volume was retained. The updated demo remains running on port 3003.

For another isolated run, choose unused host ports in shell variables before starting the [operations Compose file](../../infra/operations-demo.compose.yaml):

```powershell
$env:OPERATIONS_DASHBOARD_PORT = '3003'
$env:OPERATIONS_SURVEY_PORT = '19081'
$env:OPERATIONS_RESTORE_PORT = '19082'
$env:FAULTLINE_API_KEY = 'choose-a-local-operator-key'
docker compose -p faultline-release-check -f infra/operations-demo.compose.yaml up --build -d
```

Use an empty isolated database to load the operations scenario. Set `BASE_URL`, `SURVEY_API_URL`, `RESTORE_API_URL`, `SURVEY_OPENAPI_URL`, and `RESTORE_OPENAPI_URL` to those selected ports before running the package's contract/security/browser scripts. Install the matching browser with `node dashboard/node_modules/playwright/cli.js install chromium`. To record, set `BASE_URL` explicitly and run `node scripts/record-walkthrough.cjs` **before** recovering that fixture; recording performs real confirmed UI actions. Output defaults to ignored `private/walkthrough`.

## Remaining choices and deliberate limits

The owner still needs to select a source licence before an intended public release and review/commit the accumulated working-tree changes. A hosting provider/access model has not been selected and no public deployment or repository push was performed. The dashboard has no login and remains a local operator console. Terminal redrive, real SAP/CPI adapters, live topology telemetry, durable downstream deduplication across restart, and distributed SSE are deferred scopes. The owner's learning and rehearsal remain part of finishing the project personally; start with the learning path rather than replaying the original build schedule.

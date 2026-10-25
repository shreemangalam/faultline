# Running and verifying Faultline

Use Java 21+, Maven 3.9+, Node.js 24, and Docker Compose with Linux containers. Run commands from the repository root. The [main README](../README.md) covers the normal development stack; [CONTRIBUTING](../CONTRIBUTING.md) covers backend and dashboard builds.

## Isolated operations demo

Initialize a local environment file if one does not exist and choose an operator key of at least 16 characters:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
# Set FAULTLINE_API_KEY in .env before starting.
docker compose --env-file .env -p faultline-operations -f infra/operations-demo.compose.yaml up --build -d
```

The dashboard defaults to `http://localhost:3002`; Survey/Restore use host ports 18081/18082. The operations project owns a separate PostgreSQL volume. Set `OPERATIONS_DASHBOARD_PORT`, `OPERATIONS_SURVEY_PORT`, and `OPERATIONS_RESTORE_PORT` to unused ports when another stack occupies the defaults.

Keep `RESTORE_WORKER_ENABLED=false` for manual recovery. In Restore, expand Demo tools and confirm **Load operations demo** only on an empty isolated database. The loader refuses nonempty databases with 409 and does not replace existing records.

The fictional 39-message scenario includes an open Salesforce circuit, blocked order branches, earlier successes/retries, and permanent/dependency failures. Loading performs real simulator HTTP requests and observes retry backoff.

1. Inspect the Salesforce 503 attempt and a blocked message's prerequisite.
2. Confirm **Recover Salesforce endpoint**. This changes simulator behavior; it does not bypass cooldown or execute the queue.
3. After cooldown, confirm **Run next batch**. Each dashboard batch attempts at most five admitted messages. A probe tests the target before regular admission resumes.
4. Run further batches to release dependent work. Inspect HTTP outcomes, audit transitions, statistics, and live updates.
5. Retain terminal failures for investigation; they do not automatically redrive.

Direct mutation requests require `X-Faultline-Api-Key` with the environment's selected key. The dashboard supplies its key on the server. See [API security](technical/api-security.md) and the generated [Survey](api/survey-openapi.json)/[Restore](api/restore-openapi.json) contracts.

## Data preservation

Stop containers with `docker compose ... stop` or `down` without deleting volumes. To repeat with a clean database, use a new project name and unused ports, retaining the prior project's volume. Do not remove/prune an existing volume to reset its presentation.

Queue/history/circuits survive service restart in PostgreSQL. Simulator settings and accepted-key counters do not; its default behavior after restart is success. This demonstration does not establish durable downstream deduplication across restarts.

## System checks

Start a fresh isolated stack and load its operations fixture before browser checks. Configure URLs to its actual ports:

```powershell
$env:BASE_URL = 'http://localhost:3002'
$env:SURVEY_API_URL = 'http://localhost:18081'
$env:RESTORE_API_URL = 'http://localhost:18082'
$env:SURVEY_OPENAPI_URL = 'http://localhost:18081/v3/api-docs'
$env:RESTORE_OPENAPI_URL = 'http://localhost:18082/v3/api-docs'
# Set FAULTLINE_API_KEY to the same operator key used by the stack.
node dashboard/node_modules/playwright/cli.js install chromium
npm run contracts:check --prefix dashboard
npm run qa:security --prefix dashboard
npm run qa:browser --prefix dashboard
```

Browser checks create Survey reports; run them against an isolated database. For intentional API changes, export/review the contracts before checking them. CI verifies the backend, dashboard, security boundary, contracts, and browser behavior in an isolated Compose project.

To record real UI recovery, set `BASE_URL` explicitly to an isolated 39-event fixture before recovering it, then run `node scripts/record-walkthrough.cjs`. It performs confirmed UI actions and checks second-tab SSE updates. Output defaults to ignored `private/walkthrough`.

For throughput/accounting verification use [the benchmark procedure](technical/restore-recovery-benchmark.md). The [100-event smoke result](evidence/restore-smoke-2026-10-18.json) and historical 50,000-event measurement document their own conditions; neither is a production capacity guarantee.

## Troubleshooting

| Symptom | Inspect |
|---|---|
| No events admitted | Prerequisites, retry timestamp, circuit cooldown/probe lease, and queue eligibility |
| Loader returns 409 | Existing records; select a new isolated environment rather than clearing data |
| Direct mutation returns 401 | Selected API key and header; a separate downstream needs its own explicit credential |
| UI shows reconnecting | SSE/proxy delivery and REST snapshot responses; the database remains authoritative |
| Target set to success but work does not run | Cooldown, backoff, and whether a manual/scheduled batch was actually invoked |
| Terminal record stays stopped | Intended behavior; operator redrive is deferred |

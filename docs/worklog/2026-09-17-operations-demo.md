# Operations demo

The opt-in operations scenario creates 39 fictional messages through normal intake and real HTTP execution. Three Salesforce 503 responses open the persisted circuit. Four order chains leave inventory, payment, invoice, shipment and confirmation work blocked. Other records include 13 successful deliveries, a successful WMS retry, one malformed customer update and its terminal dependant. All timestamps, attempts, statistics and audit evidence come from execution; loading takes roughly 5–10 seconds because it observes real backoff.

Start the isolated stack:

```powershell
docker compose --env-file .env -p faultline-operations -f infra/operations-demo.compose.yaml up -d --build
# Use the same key chosen in .env, not a literal placeholder.
$operatorHeaders = @{ 'X-Faultline-Api-Key' = 'your-selected-local-key' }
Invoke-RestMethod -Method Post -Headers $operatorHeaders http://localhost:18082/api/v1/scenarios/operations-demo
```

Open http://localhost:3002. The development dashboard remains on port 3001 when configured there. This stack uses its own named volume and never mounts the preserved development database. Both workers remain disabled. Loading is explicitly opt-in and returns 409 for any nonempty database, including a repeated or interrupted load. It never deletes or overwrites existing events. To repeat from scratch, use a new Compose project name after stopping the old project's containers without deleting volumes.

Recover the simulated Salesforce endpoint, then run manual bounded batches after circuit cooldown:

```powershell
Invoke-RestMethod -Method Post -Headers $operatorHeaders http://localhost:18082/api/v1/scenarios/operations-demo/recover
```

This changes simulator behavior only. Circuit admission, the single half-open probe and dependency release remain production policies. Simulator behavior is process-local; after a service restart its default is success, while PostgreSQL queue/circuit evidence persists. The scenario loader is intended for a single local demo instance without concurrent manual execution during loading.

Verification: three new scenario tests cover exact state/attempt accounting, real HTTP results, single-probe recovery, dependency release, nonempty-data protection and disabled-mode refusal. Full Maven verification passes (69 tests). Both Compose configurations validate.

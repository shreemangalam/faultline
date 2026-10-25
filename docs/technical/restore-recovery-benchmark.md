# Restore 50,000-event recovery benchmark

`scripts/benchmark-restore.ps1` runs Restore and PostgreSQL in an isolated Compose project, proves outage containment, and then measures recovery through the public worker API. The script removes its dedicated containers and volume after the run unless `-KeepEnvironment` is supplied.

The measured run used the Java 21 service image, PostgreSQL 16, eight concurrent workers, ten-event claim batches, the in-process HTTP downstream simulator, and zero retry jitter for repeatability. It loaded one unique target with 50,000 eligible events, executed ten `503` attempts, verified that the open circuit made the next claim empty, changed the target to success, waited for the production 30-second cool-down, and drained the queue.

```powershell
./scripts/benchmark-restore.ps1 -EventCount 50000 -ParallelWorkers 8
```

## Result

Measured locally with Docker Desktop from revision `3698e39` on 2026-09-14:

| Measure | Result |
|---|---:|
| Scenario load | 7.916 s |
| End-to-end recovery after target restoration, including circuit cool-down | 272.245 s |
| Active queue drain | 242.190 s |
| Active drain rate | 206.45 events/s |
| Nearest-rank p95 receipt-to-success latency | 272.041 s |
| Attempts and simulator requests | 50,010 |
| Unique accepted deliveries | 50,000 |
| Pending / terminal failures | 0 / 0 |
| Final circuit state | `CLOSED` |

The ten extra attempts are the already-leased outage batch. The circuit opens on its third transient failure, bounds overshoot to that batch, rejects the next claim, and later admits one half-open probe before normal parallel recovery resumes. Exact attempt and accepted-delivery counts prove at-least-once execution did not create duplicate simulator side effects.

The machine-readable run output is stored in [`docs/evidence/restore-benchmark-2026-09-14.json`](../evidence/restore-benchmark-2026-09-14.json). These are reproducible demonstration numbers, not a production capacity claim. They include application, HTTP, transaction, and local Docker overhead on one machine; different hardware and runtime settings will produce different results.

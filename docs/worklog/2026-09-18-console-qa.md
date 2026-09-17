# Console visual and interaction QA

- Validated the production Survey, Restore and operator-guide pages at 1440×900, 1366×768, 768×1024 and 390×844 in light and dark themes. No horizontal page overflow or uncaught browser errors were observed.
- Verified keyboard topology selection, distinct page titles, theme persistence, selected rows, counted filters, search, queue expansion, prerequisite navigation, dialog focus/Escape handling and reduced-motion styling.
- Exercised real empty/loading states through the confirmed 39-event loader, plus browser-local transport failures, retained snapshots, persistent action errors and SSE reconnect followed by a REST refresh.
- A confirmed bounded action in an isolated run admitted exactly one probe. A second tab updated its attempt evidence through SSE invalidation. Terminal failures and the WMS HTTP 503 → 200 retry were inspected against real PostgreSQL evidence.
- Reduced topology height for desktop screens, differentiated upstream paths, retained priority on mobile, shortened generated proof labels and kept complete identifiers in technical evidence.

## Runtime and preservation

The final showcase runs at http://localhost:3002 under Compose project `faultline-showcase`, using the tested operations-demo configuration and a new dedicated volume. It was freshly loaded through the UI with 39 messages, 18 attempts, 20 blocked messages, 13 completed deliveries, two terminal failures and an OPEN Salesforce circuit. Deadlines use real time and will age normally. The earlier `faultline-operations` project is stopped; its volume is retained.

The original dashboard remains at http://localhost:3001. The original 135 records retain their exact supplied state distribution. Five unrelated scenario submissions during the session added 218 records to that development database, bringing its total to 353; no records were removed or rewritten to improve presentation. Read-only browser validation retained this total.

Both running Restore services have `RESTORE_WORKER_ENABLED=false`. Both Compose configurations validate. TypeScript and production builds pass. The backend slice passed the full 69-test Maven reactor; the subsequent SSE header fix passed its lifecycle regression test and compressed-client proxy check.

## Repeat browser verification

`scripts/verify-dashboard.cjs` requires Playwright and Chromium. Set `PLAYWRIGHT_MODULE` and `PLAYWRIGHT_EXECUTABLE` when using bundled runtimes, and optionally set `BASE_URL` and `QA_OUTPUT`. It is read-only for Restore; Survey analysis creates an incident report. Transport-failure checks intercept requests only inside the test browser; no mock data is shipped in the application.

```powershell
$env:BASE_URL = 'http://localhost:3002'
node scripts/verify-dashboard.cjs
```

Representative captures from the actual showcase:

- [Restore, light](../evidence/2026-09-18/restore-light.png)
- [Restore, dark](../evidence/2026-09-18/restore-dark.png)
- [Survey, dark](../evidence/2026-09-18/survey-dark.png)
- [Operator guide](../evidence/2026-09-18/operator-guide.png)

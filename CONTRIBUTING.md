# Developing Faultline

Use Java 21+, Maven 3.9+, Node.js 24, and Docker Compose with Linux containers. CI and backend images target Java 21. Run commands from the repository root.

```powershell
mvn --batch-mode --no-transfer-progress verify
npm ci --prefix dashboard
npm run typecheck --prefix dashboard
npm run build --prefix dashboard
```

Backend integration tests use isolated PostgreSQL Testcontainers databases and require Docker. Start local development with `docker compose --env-file .env -f infra/compose.yaml up --build -d` after copying `.env.example` to `.env` and selecting your local key. See [API security](docs/technical/api-security.md) and [operations and verification](docs/operations.md).

Keep automatic recovery disabled unless you intend to execute eligible work. Scenario loaders and browser Survey assessments write real fictional records. Use an isolated Compose project for system checks. Preserve existing database volumes; do not reset/prune them to repair a demo. The operations loader deliberately refuses nonempty databases.

Keep Java domain algorithms independent of HTTP and persistence where possible. Recovery policy belongs in the backend. Changes to worker ownership, transactions, circuits, or idempotency need focused regression coverage against realistic failure paths. For API changes, export and review both OpenAPI snapshots from the updated runtime, then run `contracts:check`, `qa:security`, and `qa:browser` from `dashboard/package.json`.

Use only fictional data and non-sensitive examples in public files. Keep `docs/` limited to current architecture, setup, APIs, operational contracts, and supporting benchmark/demo evidence. Keep local notes and temporary artifacts in ignored directories. Local environment files, credentials, and generated outputs must not be added to Git or image build contexts. Licence selection remains the repository owner's decision; do not assume a licence that is not supplied.

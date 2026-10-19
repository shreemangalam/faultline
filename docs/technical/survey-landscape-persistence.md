# Survey landscape persistence

Survey now stores a validated landscape import as an immutable snapshot. `POST /api/v1/landscape/imports` creates a snapshot and returns `201 Created`; `GET /api/v1/landscape/imports/{importId}` reads it back.

Flyway owns the schema. The first migration creates an import header plus systems and integrations keyed to that import. The write service validates defensively, writes all three record groups in one transaction, and retrieves the stored result through repositories ordered by stable identifiers.

The local runtime uses PostgreSQL configuration supplied through environment variables. Current integration tests use isolated PostgreSQL Testcontainers databases and apply the same Flyway migrations. Saving does not change analysis topology: [explicit activation](survey-active-landscape.md) selects the stored snapshot. Until an import is activated, graph reads use the bundled sample.

Both services share the default database in Compose and own independently prefixed tables/history. Survey and Restore baseline at version zero when their history is absent in an already-populated schema, then apply all their own migrations. This allows either service to initialize first without skipping V1 or changing the other service's tables. `SharedDatabaseStartupTest` loads deployment configuration and verifies this with pre-existing unrelated table data.

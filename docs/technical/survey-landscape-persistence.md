# Survey landscape persistence

Survey now stores a validated landscape import as an immutable snapshot. `POST /api/v1/landscape/imports` creates a snapshot and returns `201 Created`; `GET /api/v1/landscape/imports/{importId}` reads it back.

Flyway owns the schema. The first migration creates an import header plus systems and integrations keyed to that import. The write service validates defensively, writes all three record groups in one transaction, and retrieves the stored result through repositories ordered by stable identifiers.

The local runtime uses PostgreSQL configuration supplied through environment variables. Tests use an isolated H2 database in PostgreSQL compatibility mode and apply the same Flyway migration. The seeded in-memory graph remains the analysis source for now; a later activation step will select a persisted snapshot explicitly rather than replacing active topology implicitly.

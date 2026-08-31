# Survey landscape persistence sprint

## Outcome

Added JPA, Flyway, PostgreSQL runtime configuration, and an immutable persisted landscape snapshot. A validated request can now be created and retrieved through the database boundary.

## Verification scope

- Flyway migration and JPA mappings run against H2 in PostgreSQL compatibility mode;
- HTTP integration test proves create then retrieve using the persisted identifier;
- the full Maven suite is required before commit.

## Runtime note

The local PostgreSQL Compose service requires Docker Desktop. It was unavailable during this sprint, so real PostgreSQL startup remains a required local verification when Docker is running.

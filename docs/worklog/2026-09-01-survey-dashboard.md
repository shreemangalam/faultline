# Survey dashboard and local runtime

- Added the Next.js/TypeScript/D3 Survey dashboard with a real API proxy, graph interaction, loading/error states, incident creation, and persisted-report display.
- Added multi-stage Dockerfiles and Compose services for PostgreSQL, Survey, and the dashboard; Docker context excludes local artifacts.
- Verified TypeScript and production Next.js builds, valid Compose configuration, and the live Docker stack: Survey health is UP; a persisted impact report round-trips through PostgreSQL; the dashboard and its internal API proxy return 200 with 16 systems and 35 integrations.
- Upgraded Next.js to 16.3.4 to remediate its bundled PostCSS advisories. The production dependency audit is clean, and the rebuilt image still proxies the live Survey landscape.

- Reworked the dashboard into a restrained operations console: persisted light/dark preference, dense incident inspector, directional D3 edges, criticality-scaled nodes, and selected-neighbourhood focus. Type checking, production build, and rebuilt live proxy verification pass.

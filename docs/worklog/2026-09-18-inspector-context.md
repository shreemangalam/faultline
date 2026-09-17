# 2026-09-18 — Inspector context

- Keep the inspected message within the active search and state results; empty results clear the inspector.
- Reset inspector scroll and expanded technical evidence when the inspected message changes, while preserving reading position on refresh of the same message.
- Following prerequisites or the deadline callout reveals the referenced message, clearing incompatible filters and expanding the visible queue when necessary.
- Validation: TypeScript and production Docker build passed. `scripts/verify-inspector.cjs` passed against the real isolated demo, covering scroll, refresh, empty search, state filtering and prerequisite navigation. No backend writes; development/showcase event counts remained 353/39.

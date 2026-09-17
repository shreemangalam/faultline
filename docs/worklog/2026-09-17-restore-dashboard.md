# Restore dashboard

- Added `/restore` with queue filters, operational statistics, circuit state, a combined audit/attempt timeline, and controls for the four repeatable scenarios plus one bounded worker batch.
- Added Survey/Restore module navigation and a same-origin Restore proxy, including SSE passthrough. Live notifications are debounced into REST snapshot refreshes.
- Made the dashboard port configurable in Compose and validated the container on port 3001 without enabling the automatic worker.
- Verification: `npm run typecheck`, `npm run build`, live Survey and Restore proxy requests, proxied `stream-ready`, and the preserved 135-event row count.
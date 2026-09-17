# Restore operations console

- Replaced equal metric tiles with a queue/incident summary, unhealthy-target emphasis, deadline context and compact circuit cards.
- Added business-readable payload labels, correlation search, counted state filters, incremental queue display and a message inspector with prerequisites, delivery attempts, HTTP results, timestamps and raw technical evidence.
- Separated demo tools from monitoring and added keyboard-accessible native confirmation dialogs. Actions remain bounded; SSE still only schedules debounced REST snapshots.
- Verification: TypeScript and production build pass. The isolated real database contains 39 events; the preserved development database remains at 135 with workers disabled.

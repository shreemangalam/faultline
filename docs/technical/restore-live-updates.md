# Restore live-update contract

> Status: implemented and verified. Recovery-event delivery is asynchronous and bounded so client I/O cannot delay durable recovery work.

`GET /api/v1/stream` returns `text/event-stream`. A new connection receives `stream-ready`; the browser must then fetch the REST snapshot. Later committed event transitions arrive as `recovery-event` records with event ID, correlation ID, target system, state, and occurrence time.

Publication uses an application event raised inside the write transaction and a listener in the `AFTER_COMMIT` phase. Rolled-back state is therefore never presented as current. The listener enqueues delivery onto one daemon worker backed by a configurable bounded buffer (256 notifications by default), so a slow or disconnected client cannot delay a committed mutation response. Retained notifications are delivered in order; when the buffer is full, the oldest pending invalidation is discarded. The stream covers intake, claim and lease reclaim, retry scheduling, exhaustion, permanent failure, completion, dependency release, and dependency failure propagation.

The connection sends a comment heartbeat every 15 seconds, disables proxy buffering through `X-Accel-Buffering: no`, and has a configurable 30-minute server timeout. Browser reconnection is expected.

SSE is a process-local notification channel, not the system of record. Sequence IDs restart with the process and the server does not replay missed notifications. After connecting or reconnecting, the dashboard refreshes events, statistics, and circuits from REST; PostgreSQL remains authoritative. A horizontally scaled deployment would replace or complement the in-process publisher with a shared event transport before every replica could serve equivalent streams.

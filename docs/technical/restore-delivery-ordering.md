# Restore delivery and ordering contract

Restore provides **at-least-once delivery with explicit dependency ordering**, not global FIFO.

Within one correlation, a dependant cannot be claimed until every declared prerequisite is `COMPLETED`. Terminal prerequisite failure propagates to blocked descendants. Independent events and sibling branches have no implicit order and may execute concurrently, even when they share a correlation or target. Claim order is also not completion order: downstream latency and concurrent workers can finish later-ranked work first.

Eligible admission uses this deterministic ranking:

1. lower business-priority number;
2. earlier non-null deadline;
3. more currently blocked direct dependants;
4. fewer prior attempts;
5. earlier creation time, then event UUID.

The third rule favours an event whose completion may unlock more immediate work. It is a heuristic, not a dependency guarantee. `FOR UPDATE SKIP LOCKED` preserves exclusive claims while allowing workers to progress independently, so a worker skips candidates another transaction already owns instead of waiting to reproduce one global ordering.

Submission deduplicates on the caller's idempotency key and rejects the same key with changed input. Delivery uses the stable event UUID as the downstream idempotency key. A worker crash or ambiguous timeout can therefore cause another attempt without causing a second accepted side effect at an idempotent downstream target.

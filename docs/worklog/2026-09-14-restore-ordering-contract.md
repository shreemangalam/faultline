# Restore ordering contract

- Added blocked direct-dependant count to deterministic claim ranking after priority and deadline, before prior-attempt cost and stable creation/UUID ties.
- Added an HTTP integration test that proves all four business ranking dimensions through the production claim query.
- Documented the precise boundary: at-least-once delivery with explicit dependency partial order, no global FIFO, and no promised completion order between independent branches.
- Verified seven focused worker tests, the full 61-test reactor, and an isolated PostgreSQL outage/recovery run; the benchmark-only resources were removed afterward.

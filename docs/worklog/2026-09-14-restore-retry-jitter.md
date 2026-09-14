# Restore retry jitter

- Replaced the fixed retry-delay helper with a dedicated capped exponential-backoff policy and an injectable jitter source.
- Retry attempts now add a configurable inclusive 0–2 second jitter window to the active 5s and 10s retry bases; the third failure exhausts, while the policy remains capped at 60s for extension. Zero jitter remains available for controlled scenarios.
- Unit tests cover minimum, maximum, cap, zero-window, and invalid input behavior. An integration test proves the chosen delay is persisted from the attempt finish time.
- Verified 32 focused Restore tests and the full 54-test Maven reactor. A rebuilt Java 21 container persisted a 5,028 ms retry after one simulated `503`, within the configured 5,000–7,000 ms range.

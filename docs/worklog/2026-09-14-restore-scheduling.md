# Restore automatic scheduling

- Added an opt-in fixed-delay worker that executes bounded recovery batches without an operator calling the manual endpoint.
- Added validated worker identity, batch-size, and lease-duration configuration. A process-local guard skips overlapping ticks, and a failed batch does not prevent later runs.
- Kept scheduling disabled by default so starting a developer stack cannot deliver preserved queue data unexpectedly; clean demo and deployment environments enable it explicitly.
- Verified 27 focused Restore tests and the full 49-test Maven reactor. Rebuilt the Java 21 Restore image, confirmed health, and verified that all 21 preserved local events retained the same states and attempt counts.

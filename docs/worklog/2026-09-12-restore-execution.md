# Restore execution

- Added the lease-owner-guarded `FAILED_FINAL` transition for non-retriable work.
- Permanent failure closes the active attempt as `PERMANENT_FAILURE`, clears the lease, and does not increment the target circuit because validation defects are not evidence of target instability.
- Verified 12 focused Restore tests, the full 34-test Maven reactor, a rebuilt Restore container, and a live PostgreSQL transition with the target circuit remaining closed at zero failures.
- Automatic HTTP outcome classification and downstream simulation remain the next slice.

# Survey landscape warnings

- Added `GET /api/v1/landscape/warnings` for configuration-time graph anomalies.
- The endpoint applies Kahn-style elimination to the active topology and reports nodes that cannot receive a dependency-safe order.
- Verified against the bundled WMS/inventory cycle with 22 shared/Survey tests.
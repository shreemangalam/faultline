# Survey active landscape

A validated import is immutable historical configuration until an operator explicitly activates it. `PUT /api/v1/landscape/imports/{importId}/activate` atomically selects that stored snapshot as Survey's active topology. `GET /api/v1/landscape/active` exposes the selected snapshot.

The health map, impact analysis, priority report, and recovery planner all resolve the same active topology. If no imported snapshot has been activated, they use the bundled fictional landscape. This avoids hidden topology changes and makes a report reproducible against a named import.

The selection is a singleton relational record. The implementation deliberately stores a snapshot identifier rather than duplicating systems and edges or mutating a previously stored import.
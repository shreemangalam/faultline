# Survey active-landscape selection

- Added an explicit active-topology selection backed by a Flyway migration and immutable persisted imports.
- Existing Survey graph endpoints now resolve the active snapshot consistently and fall back to the bundled demo when no snapshot is selected.
- Added HTTP coverage for activation, unknown imports, and the switched health-map/impact behavior. The stateful test clears its singleton selection after each run.
- Verified with the complete Maven reactor (`21` tests).
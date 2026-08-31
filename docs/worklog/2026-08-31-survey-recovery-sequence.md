# Survey recovery sequence sprint

## Outcome

Added the recovery-sequence API, a framework-independent Kahn planner, and explicit cycle-warning output. The planner returns only systems whose prerequisites are resolved and leaves the remaining affected nodes visible for operator action.

## Verification scope

- algorithm tests cover deterministic ordering and a reachable cycle;
- the HTTP contract test covers the seeded SAP ECC outage;
- the full Maven verification suite and a live local HTTP request are required before commit.

## Next boundary

The current sample has no cycle because the API needs a useful normal-path demo. A later sample/import slice will add a labelled legacy cycle for end-to-end warning demonstration.

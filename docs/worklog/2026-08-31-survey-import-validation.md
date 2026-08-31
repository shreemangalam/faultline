# Survey import-validation sprint

## Outcome

Added a read-only import validation endpoint that reports all detectable client errors together. This establishes the boundary that a later landscape import and persistence layer will reuse.

## Verification scope

- pure validator tests cover a valid graph and multiple simultaneous errors;
- HTTP tests cover both `200` valid and `400` invalid contracts;
- the full Maven suite and a live API call are required before commit.

## Boundary

Validation does not replace the active landscape. The subsequent persistence sprint will make a successful import durable and visible to analysis APIs.

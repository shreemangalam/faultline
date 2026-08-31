# Survey landscape metadata

Survey integration edges carry an explicit `dataDomain` and `slaClass` in addition to transport protocol. The fields are persisted with each imported snapshot and appear in the impact and health-map responses.

`dataDomain` lets impact classification distinguish a true alternate feed from an unrelated integration into the same target. `slaClass` supplies the urgency input to priority scoring; protocol is transport, not a reliable SLA proxy.

The bundled fictional landscape now contains 16 systems and 35 flows. It includes a reachable legacy WMS/inventory cycle. Kahn planning returns its safe prefix and flags the unresolved cycle instead of inventing an order.
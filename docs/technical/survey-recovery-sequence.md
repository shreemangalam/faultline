# Survey recovery sequence

`POST /api/v1/analysis/recovery-sequence` creates a dependency-safe order for the failed system and all systems reached by the impact analysis. It uses Kahn's topological-sort algorithm over that affected subgraph.

An integration from system A to system B means B depends on A. Systems with no remaining prerequisites are emitted first. A lexical system-ID tie-breaker makes the output stable when more than one system is ready at once.

If Kahn's algorithm cannot emit every affected system, the response sets `cycleWarning` and lists `unresolvedSystemIds`. Those IDs include nodes in a cycle and any node still waiting behind that cycle; they are not presented as an automatic recovery order. The returned sequence is only the safe acyclic prefix.

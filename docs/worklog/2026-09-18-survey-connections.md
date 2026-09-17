# 2026-09-18 — Survey connection tracing

- Survey opens with the selected system between its direct upstream and downstream paths. Larger cards expose system category, recorded health and criticality; edges show protocol and explicit data direction.
- Full landscape remains one click away. Hover no longer changes graph focus independently of the inspector. Same-lane routes use a gutter and opposite-direction links use separate ports.
- Reciprocal neighbors appear on both sides with explanatory text. Direct paths are explicitly distinguished from backend incident impact analysis; no recovery or impact policy moved into the browser.
- On phones, the focused view becomes a vertical sequence with labeled directions and keyboard-operable system cards. The full landscape remains horizontally scrollable.

Validation:

- TypeScript and production Docker build passed. Both Compose configurations validate.
- `scripts/verify-survey-graph.cjs` checked every one of the 16 system neighborhoods against the real API, the 35-path full landscape, reciprocal links, selection/hover consistency, keyboard focus retention, label bounds, and both themes at 1440×900, 1366×768, 768×1024 and 390×844.
- Inspector regressions and the complete dashboard browser suite passed. Both running dashboards use the validated image, with real Survey/Restore proxies and successful SSE reconnect tests.
- Restore event totals remained 353 in development and 39 in the isolated showcase. Both scheduled workers remain disabled. No backend code or Restore data changed.

Evidence: [desktop light](../evidence/2026-09-18/survey-focus-light.png), [desktop dark](../evidence/2026-09-18/survey-focus-dark.png), [mobile](../evidence/2026-09-18/survey-focus-mobile.png).

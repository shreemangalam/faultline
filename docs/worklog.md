# Faultline worklog

This is a concise, versioned record of shipped engineering decisions. It is intended to help a maintainer or coding agent understand what changed, why it changed, and how it was verified.

## 2026-08-31 — Survey seeded impact analysis

- Shipped a fictional landscape loaded from `survey/src/main/resources/landscape.json` and `POST /api/v1/analysis/impact`.
- Added a framework-independent breadth-first traversal that groups direct and cascade effects into impact rings and remains finite when a reachable cycle exists.
- Verified with focused algorithm and HTTP tests, the full Maven verification suite, and a live local HTTP request.
- Commit: `995c0dc feat(survey): add seeded impact analysis slice`.

## 2026-08-31 — Survey impact priority

- Added an explainable priority report for the seeded outage, including score components, severity band, and deterministic ranking.
- Kept raw reachability and prioritisation as separate API responses so the dashboard can display either view without reconstructing policy in the browser.
- Added algorithm and HTTP contract tests. Verification and commit details will be recorded after this sprint completes.

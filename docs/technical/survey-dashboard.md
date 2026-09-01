# Survey dashboard

The `dashboard/` Next.js application renders the active Survey topology as a structured, lane-based SVG board. Nodes are clickable and keyboard-selectable. The page fetches the real health map and topology warnings, submits a complete-outage incident, and renders the report returned by Survey.

Browser requests use `/api/survey/...`; Next.js rewrites them to `SURVEY_API_URL` (default `http://localhost:8081`). This keeps the browser same-origin and leaves graph reasoning and policy in Java.

Run the full local demo from the repository root:

```powershell
docker compose -f infra/compose.yaml up --build
```

Open `http://localhost:3000`; Survey is available on `http://localhost:8081`. For frontend-only development, run `npm run dev` in `dashboard/` with Survey already running.
The interface uses a dark-first operations-console treatment with a persisted light-mode choice. The topology board uses operational lanes for business inputs, core systems, CPI flows, and channels. It renders directed paths, criticality markers, and the selected system's immediate dependency neighbourhood without losing the wider landscape.

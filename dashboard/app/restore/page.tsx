"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

type EventState = "RECEIVED" | "ELIGIBLE" | "BLOCKED" | "IN_PROGRESS" | "RETRY_SCHEDULED" | "COMPLETED" | "EXHAUSTED" | "FAILED_FINAL" | "FAILED_DEPENDENCY";
type Attempt = { attemptId: string; attemptNumber: number; workerId: string; outcome: string; failureReason: string | null; httpStatus: number | null; durationMillis: number | null; startedAt: string; finishedAt: string | null };
type Audit = { auditId: string; toState: EventState; reason: string; occurredAt: string };
type RecoveryEvent = {
  eventId: string; correlationId: string; targetSystem: string; payloadReference: string; priority: number;
  deadlineAt: string | null; state: EventState; attemptCount: number; nextEligibleAt: string;
  leaseOwner: string | null; leaseExpiresAt: string | null; createdAt: string;
  prerequisiteEventIds: string[]; audits: Audit[]; attempts: Attempt[];
};
type Statistics = { generatedAt: string; totalEvents: number; pendingEventCount: number; terminalFailureCount: number; eventCounts: Record<EventState, number>; totalAttempts: number; attemptCounts: Record<string, number>; p95SuccessfulRecoveryLatencyMillis: number | null };
type Circuit = { targetSystem: string; state: "CLOSED" | "OPEN" | "HALF_OPEN"; consecutiveFailures: number; openUntil: string | null; updatedAt: string };
type DemoStatus = { enabled: boolean; eventCount: number };
type Action = { name: string; path: string; description: string; body?: object };
const api = "/api/restore/v1";
const terminalStates = new Set<EventState>(["COMPLETED", "EXHAUSTED", "FAILED_FINAL", "FAILED_DEPENDENCY"]);
const filters: Array<"ALL" | EventState> = ["ALL", "ELIGIBLE", "BLOCKED", "RETRY_SCHEDULED", "IN_PROGRESS", "COMPLETED", "FAILED_FINAL", "FAILED_DEPENDENCY", "EXHAUSTED", "RECEIVED"];
const labels: Record<string, string> = { ALL: "All events", ELIGIBLE: "Ready", BLOCKED: "Blocked", RETRY_SCHEDULED: "Retry waiting", IN_PROGRESS: "In progress", COMPLETED: "Completed", FAILED_FINAL: "Final failure", FAILED_DEPENDENCY: "Dependency failed", EXHAUSTED: "Exhausted", RECEIVED: "Received", OPEN: "Open", HALF_OPEN: "Probe in progress", CLOSED: "Closed" };
const targets: Record<string, string> = { SALESFORCE: "Salesforce", LEGACY_WMS: "Legacy WMS", SAP_S4_FINANCE: "SAP S/4 Finance", BANK_GATEWAY: "Bank Gateway", CARRIER_API: "Carrier API", CUSTOMER_PORTAL: "Customer Portal" };
const steps: Record<string, string> = { "cpi-order-sync": "CPI order synchronization", "inventory-reservation": "Inventory reservation", "payment-capture": "Payment capture", "invoice-posting": "Invoice posting", "shipment-booking": "Shipment booking", "order-confirmation": "Order confirmation", "customer-update-invalid": "Customer update · invalid payload", "customer-confirmation": "Customer confirmation", "order-settlement": "Order settlement" };
function targetName(value: string) {
  if (targets[value]) return targets[value];
  const proof = value.match(/^(OUTAGE|ERP_PRIORITY|LIVE_CIRCUIT|PERMANENT_PROBE|CRM_INVALID|ERP_UNAVAILABLE|CRM|ERP|WMS|PAYMENTS|TMS|OPERATIONS|NOTIFICATIONS|ERP_RECON)_([A-F0-9]+)$/);
  if (proof) {
    const names: Record<string, string> = { OUTAGE: "Outage simulator", ERP_PRIORITY: "Priority demo", LIVE_CIRCUIT: "Circuit proof", PERMANENT_PROBE: "Permanent-failure proof", CRM_INVALID: "Validation demo", ERP_UNAVAILABLE: "Unavailable ERP" };
    return (names[proof[1]] ?? proof[1].replaceAll("_", " ") + " demo") + " · " + proof[2].slice(0, 4);
  }
  return value.replaceAll("_", " ");
}
function messageName(event: RecoveryEvent) {
  const step = event.payloadReference.split("/").at(-1) ?? "";
  return event.payloadReference.startsWith("demo://operations/") ? steps[step] ?? step : "Message to " + targetName(event.targetSystem);
}
function correlationName(value: string) {
  return value.replace(/-([0-9a-f]{8})-[0-9a-f-]{27}$/i, " · $1").replaceAll("dependency-cascade", "Dependency cascade").replaceAll("thundering-herd", "Outage scenario").replaceAll("priority-deadline", "Priority scenario").replaceAll("terminal-failures", "Terminal scenario");
}

export default function RestoreDashboard() {
  const [events, setEvents] = useState<RecoveryEvent[]>([]);
  const [statistics, setStatistics] = useState<Statistics | null>(null);
  const [circuits, setCircuits] = useState<Circuit[]>([]);
  const [demo, setDemo] = useState<DemoStatus | null>(null);
  const [selectedEventId, setSelectedEventId] = useState<string | null>(null);
  const [filter, setFilter] = useState<"ALL" | EventState>("ALL");
  const [query, setQuery] = useState("");
  const [limit, setLimit] = useState(30);
  const [streamState, setStreamState] = useState<"connecting" | "live" | "reconnecting">("connecting");
  const [loading, setLoading] = useState(true);
  const [action, setAction] = useState<string | null>(null);
  const [confirm, setConfirm] = useState<Action | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const refreshTimer = useRef<number | null>(null);
  const requestVersion = useRef(0);
  const dialog = useRef<HTMLDialogElement>(null);
  const detail = useRef<HTMLElement>(null);
  const [now, setNow] = useState(Date.now());

  const loadSnapshot = useCallback(async () => {
    const version = ++requestVersion.current;
    try {
      const responses = await Promise.all([
        fetch(api + "/events", { cache: "no-store" }),
        fetch(api + "/statistics", { cache: "no-store" }),
        fetch(api + "/circuit-breakers", { cache: "no-store" })
      ]);
      if (responses.some(response => !response.ok)) throw new Error("Restore is unavailable. The last loaded snapshot is retained; reconnect or refresh to try again.");
      const [eventBody, statisticsBody, circuitBody] = await Promise.all(responses.map(response => response.json())) as [RecoveryEvent[], Statistics, Circuit[]];
      if (version !== requestVersion.current) return;
      setEvents(eventBody);
      setStatistics(statisticsBody);
      setCircuits(circuitBody);
      setSelectedEventId(current => current && eventBody.some(item => item.eventId === current) ? current : rankEvents(eventBody)[0]?.eventId ?? null);
      setError(null);
    } catch (reason) {
      if (version === requestVersion.current) setError("Restore is unavailable. The last loaded snapshot is retained; reconnect or refresh to try again.");
    } finally { if (version === requestVersion.current) setLoading(false); }
  }, []);

  useEffect(() => {
    void loadSnapshot();
    void fetch(api + "/scenarios/operations-demo").then(response => response.ok ? response.json() : null).then(setDemo).catch(() => setDemo(null));
    const scheduleRefresh = () => {
      if (refreshTimer.current !== null) window.clearTimeout(refreshTimer.current);
      refreshTimer.current = window.setTimeout(() => void loadSnapshot(), 120);
    };
    const source = new EventSource(api + "/stream");
    source.addEventListener("stream-ready", () => { setStreamState("live"); scheduleRefresh(); });
    source.addEventListener("recovery-event", scheduleRefresh);
    source.onerror = () => setStreamState("reconnecting");
    const clock = window.setInterval(() => setNow(Date.now()), 30000);
    return () => {
      if (refreshTimer.current !== null) window.clearTimeout(refreshTimer.current);
      source.close();
      window.clearInterval(clock);
      ++requestVersion.current;
    };
  }, [loadSnapshot]);

  useEffect(() => { if (confirm) dialog.current?.showModal(); else dialog.current?.close(); }, [confirm]);

  async function runAction(request: Action) {
    setConfirm(null);
    setAction(request.name);
    setActionError(null);
    setNotice(null);
    try {
      const response = await fetch(api + request.path, { method: "POST", headers: request.body ? { "Content-Type": "application/json" } : undefined, body: request.body ? JSON.stringify(request.body) : undefined });
      if (!response.ok) throw new Error(request.name + " could not complete (HTTP " + response.status + "). " + (request.path === "/scenarios/operations-demo" && response.status === 409 ? "Loading requires an empty isolated demo database." : "Refresh the queue before trying again."));
      const result = await response.json();
      setNotice(Array.isArray(result) ? request.name + ": " + result.length + " event(s) executed. Inspect the updated states and evidence below." : request.path.endsWith("/recover") ? "Salesforce simulator now accepts deliveries. Circuit cooldown still applies; run one bounded batch to admit a probe." : request.name + " finished. The queue snapshot has been refreshed.");
      await loadSnapshot();
      const status = await fetch(api + "/scenarios/operations-demo");
      if (status.ok) setDemo(await status.json());
    } catch (reason) { setActionError(reason instanceof Error ? reason.message : "Restore action failed."); }
    finally { setAction(null); }
  }

  const matches = useMemo(() => rankEvents(events.filter(event => (filter === "ALL" || event.state === filter) && [event.correlationId, event.targetSystem, event.payloadReference, event.eventId, messageName(event)].join(" ").toLowerCase().includes(query.trim().toLowerCase()))), [events, filter, query]);
  const visibleEvents = matches.slice(0, limit);
  const selected = matches.find(event => event.eventId === selectedEventId) ?? matches[0] ?? null;
  // Keep the inspector within the current results, including after live state changes.
  // Only changing the inspected message resets scroll; snapshot refreshes preserve it.
  useEffect(() => {
    setSelectedEventId(selected?.eventId ?? null);
    detail.current?.scrollTo({ top: 0 });
  }, [selected?.eventId]);
  const unhealthy = [...circuits].filter(circuit => circuit.state !== "CLOSED");
  const orderedCircuits = [...circuits].sort((a, b) => Number(a.state === "CLOSED") - Number(b.state === "CLOSED") || a.targetSystem.localeCompare(b.targetSystem));
  const firstUnhealthy = unhealthy[0];
  const lastFailure = firstUnhealthy && events.filter(event => event.targetSystem === firstUnhealthy.targetSystem).flatMap(event => event.attempts).filter(attempt => attempt.httpStatus && attempt.httpStatus >= 400).sort((a,b) => Date.parse(b.startedAt) - Date.parse(a.startedAt))[0];
  const urgent = rankEvents(events.filter(event => !terminalStates.has(event.state) && event.deadlineAt))[0];
  const batchAction: Action = { name: "Run next batch", path: "/worker/run-once", body: { workerId: "dashboard-worker", maxEvents: 5, leaseSeconds: 60 }, description: "Attempt at most five eligible messages using real HTTP delivery. Dependency, retry and circuit rules still apply. This does not enable the scheduled worker." };

  function selectEvent(id: string, moveFocus = false) {
    const index = matches.findIndex(event => event.eventId === id);
    if (index < 0) {
      setFilter("ALL");
      setQuery("");
      setLimit(Math.max(30, rankEvents(events).findIndex(event => event.eventId === id) + 1));
    } else if (index >= limit) setLimit(index + 1);
    setSelectedEventId(id);
    if (moveFocus || window.innerWidth < 1000) window.requestAnimationFrame(() => { detail.current?.focus(); detail.current?.scrollIntoView({ behavior: "auto", block: "start" }); });
  }

  return <main id="main-content" className="app-shell restore-shell">
    <section className="page-heading">
      <div><p className="eyebrow">Restore / Recovery operations</p><h1>Keep business moving.</h1><p className="lede">Prioritize pending messages, inspect blocked work, and recover with evidence.</p></div>
      <div className="heading-actions"><div className={"stream-state " + streamState} role="status"><span className="pulse" />{streamState === "live" ? "Live updates connected" : streamState === "reconnecting" ? "Reconnecting to updates" : "Connecting to updates"}</div><button className="button-secondary" onClick={() => void loadSnapshot()}>Refresh</button></div>
    </section>
    {error && <p className="error" role="alert">{error}</p>}
    {actionError && <p className="error" role="alert">{actionError}</p>}
    {notice && <p className="notice" role="status">{notice}</p>}
    {action && <p className="loading" role="status">{action} in progress. Demo loading observes real retry backoff and may take several seconds.</p>}
    {loading && <div className="loading" role="status">Loading recovery queue, target health and event evidence…</div>}
    {statistics && <>
      <section className={"operations-summary panel " + (unhealthy.length ? "has-incident" : "")} aria-label="Queue health">
        <div className="incident-overview"><div className="summary-kicker"><span className={"status-symbol " + (unhealthy.length ? "attention" : "healthy")}>{unhealthy.length ? "!" : "✓"}</span>{unhealthy.length ? "Target attention required" : statistics.pendingEventCount ? "Recovery work pending" : "Queue clear"}</div>
          <h2>{firstUnhealthy ? targetName(firstUnhealthy.targetSystem) + " " + (firstUnhealthy.state === "HALF_OPEN" ? "is probing recovery" : "circuit is open") : statistics.pendingEventCount ? statistics.pendingEventCount + " messages awaiting recovery" : "No pending recovery work"}</h2>
          <p>{lastFailure ? "Latest failed delivery: HTTP " + lastFailure.httpStatus + ". " : ""}{statistics.eventCounts.BLOCKED} messages are waiting on prerequisites across the queue.</p>
          <div className="summary-bottom"><strong>{statistics.pendingEventCount}<span>pending messages</span></strong><span>{statistics.eventCounts.COMPLETED} completed of {statistics.totalEvents} total<br />{statistics.eventCounts.IN_PROGRESS} in progress · {statistics.eventCounts.RETRY_SCHEDULED} retry waiting</span></div>
        </div>
        <div className="summary-measures"><div><span>Unhealthy targets</span><strong className={unhealthy.length ? "danger-text" : ""}>{unhealthy.length}<small> / {circuits.length}</small></strong><p>Open or probing</p></div><div><span>Terminal failures</span><strong className={statistics.terminalFailureCount ? "danger-text" : ""}>{statistics.terminalFailureCount}</strong><p>Manual review needed</p></div><div><span>P95 recovery time</span><strong>{statistics.p95SuccessfulRecoveryLatencyMillis === null ? "—" : formatDuration(statistics.p95SuccessfulRecoveryLatencyMillis)}</strong><p>Receipt to success · {statistics.eventCounts.COMPLETED} events</p></div></div>
      </section>

      {urgent && <div className="deadline-strip"><span className="priority">P{urgent.priority}</span><strong>{correlationName(urgent.correlationId)}</strong><span>{messageName(urgent)} · {deadlineText(urgent.deadlineAt!, now)}</span><button onClick={() => selectEvent(urgent.eventId, true)}>Inspect message →</button></div>}

      <section className="target-section" aria-label="Target circuit health"><div className="section-heading"><h2>Target health</h2><span>Persisted circuit state · {circuits.length - unhealthy.length} closed</span></div>
        <div className="target-strip">{orderedCircuits.slice(0, 6).map(circuit => <CircuitCard key={circuit.targetSystem} circuit={circuit} now={now} />)}</div>
        {circuits.length > 6 && <details className="more-targets"><summary>View {circuits.length - 6} more targets</summary><div className="target-strip">{orderedCircuits.slice(6).map(circuit => <CircuitCard key={circuit.targetSystem} circuit={circuit} now={now} />)}</div></details>}
        {circuits.length === 0 && <p className="empty-state">No target circuits yet. They appear when messages are accepted.</p>}
      </section>

      <section className="queue-workspace">
        <div className="panel queue-panel"><div className="panel-heading"><div><h2>Recovery queue <span className="count-label">{statistics.totalEvents}</span></h2><p className="panel-description">Pending first, then priority and deadline. Execution order is decided by Restore.</p></div><label className="queue-search"><span className="sr-only">Search messages</span><input value={query} onChange={event => { setQuery(event.target.value); setLimit(30); }} placeholder="Search order, target or ID" type="search" /></label></div>
          <div className="filter-row" role="group" aria-label="Filter recovery queue">{filters.filter(state => state === "ALL" || state === filter || (statistics.eventCounts[state] ?? 0) > 0).map(state => <button aria-pressed={filter === state} className={filter === state ? "active" : ""} key={state} onClick={() => { setFilter(state); setLimit(30); }}>{labels[state]} <span>{state === "ALL" ? statistics.totalEvents : statistics.eventCounts[state]}</span></button>)}</div>
          <div className="table-wrap"><table className="queue-table"><thead><tr><th scope="col">Message / business reference</th><th scope="col">State</th><th scope="col">Priority / timing</th></tr></thead><tbody>{visibleEvents.map(event => <tr key={event.eventId} className={event.eventId === selected?.eventId ? "selected" : ""} onClick={() => selectEvent(event.eventId)}>
            <td><button aria-pressed={event.eventId === selected?.eventId} aria-controls="event-evidence" onClick={click => { click.stopPropagation(); selectEvent(event.eventId); }}><span>{messageName(event)}<b className="mobile-priority">P{event.priority}</b></span><small>{correlationName(event.correlationId)}</small></button><span className="row-target">{targetName(event.targetSystem)}</span></td>
            <td><StateBadge state={event.state} /><small className="row-caption">{event.state === "BLOCKED" ? event.prerequisiteEventIds.length + " prerequisite(s)" : event.attemptCount + " attempt(s)"}</small></td>
            <td><span className={"priority " + (event.priority === 1 ? "urgent" : "")}>P{event.priority}</span><small className="row-caption">{terminalStates.has(event.state) ? "Finished" : event.deadlineAt ? deadlineText(event.deadlineAt, now) : event.state === "BLOCKED" ? "Awaiting release" : event.state === "IN_PROGRESS" ? "Worker lease active" : "Eligible " + relativeTime(event.nextEligibleAt, now)}</small></td>
          </tr>)}</tbody></table></div>
          {visibleEvents.length === 0 && <div className="empty-state"><strong>{events.length ? "No matching messages" : "Your recovery queue is empty"}</strong><p>{events.length ? "Try another state or search term." : "Load a scenario in Demo tools to explore a recovery incident."}</p>{events.length > 0 && <button className="button-secondary" onClick={() => { setFilter("ALL"); setQuery(""); setLimit(30); }}>Clear filters</button>}</div>}
          <div className="queue-footer"><span>Showing {visibleEvents.length} of {matches.length} matching messages</span>{matches.length > limit && <button className="button-secondary" onClick={() => setLimit(limit + 30)}>Show 30 more</button>}</div>
        </div>

        <aside id="event-evidence" ref={detail} tabIndex={-1} className="panel evidence-panel" aria-label="Selected event evidence">
          <div className="panel-heading"><div><p className="eyebrow">Message inspector</p><h2>{selected ? messageName(selected) : "Select a message"}</h2></div></div>
          {selected ? <><div className="evidence-reference"><strong>{correlationName(selected.correlationId)}</strong><StateBadge state={selected.state} /></div><EventDetail key={selected.eventId} event={selected} events={events} onSelect={id => selectEvent(id, true)} now={now} /></> : <p className="empty-state">{matches.length === 0 && events.length > 0 ? "No messages match the current filters. Clear or adjust the filters to inspect a message." : "Choose a queue message to inspect its prerequisites and delivery evidence."}</p>}
        </aside>
      </section>
      <div className="snapshot-note">{error ? "Last successful snapshot" : "REST snapshot"} · {formatDate(statistics.generatedAt)} · PostgreSQL is authoritative. <Link href="/help">How live updates work</Link></div>
    </>}
    <details className="panel demo-tools"><summary><span>Demo tools</span><small>Scenario data & bounded execution</small><span className="demo-safety">Explicit actions only</span></summary><div className="demo-content"><p>These actions write fictional local records. The scheduled worker is disabled in the supplied Compose configurations. Each batch attempts at most five eligible messages.</p>
      {demo?.enabled ? <div className="demo-story"><strong>Operations walkthrough</strong><p>Salesforce order outage, downstream dependencies, a WMS retry and terminal customer-update evidence. Loading requires an empty isolated database.</p><div className="scenario-actions">
        <button disabled={action !== null || (statistics?.totalEvents ?? demo.eventCount) > 0} onClick={() => setConfirm({ name: "Load operations demo", path: "/scenarios/operations-demo", description: "Create and execute a curated 39-message incident in this empty isolated database. Loading takes several seconds and cannot be undone here." })}>Load operations demo</button>
        <button disabled={action !== null || !events.some(event => event.targetSystem === "SALESFORCE")} onClick={() => setConfirm({ name: "Recover Salesforce", path: "/scenarios/operations-demo/recover", description: "Configure the fictional Salesforce endpoint to return success. This does not close its circuit or deliver messages; cooldown and a manual probe still apply." })}>Recover Salesforce endpoint</button>
      </div></div> : <p className="demo-environment-note">For the curated incident, start the isolated operations-demo stack on port 3002. Its loader is disabled on the preserved development database.</p>}
      <div className="scenario-actions">{[
        { name: "Dependency cascade", path: "/scenarios/dependency-cascade", description: "Add six new dependency-linked messages to this database." },
        { name: "Priority & deadline", path: "/scenarios/priority-deadline", description: "Add seven priority and deadline demonstration messages to this database." },
        { name: "Terminal failures", path: "/scenarios/terminal-failures", description: "Add five messages configured to demonstrate permanent and exhausted failures." },
        { name: "100-event outage", path: "/scenarios/thundering-herd", body: { eventCount: 100 }, description: "Add 100 messages against a new unavailable simulator target. These remain in the local database." },
        batchAction
      ].map(request => <button key={request.name} className={request.name === batchAction.name ? "primary" : ""} disabled={action !== null || !statistics} onClick={() => setConfirm(request)}>{request.name}</button>)}</div>
    </div></details>
    <dialog ref={dialog} className="action-dialog" onCancel={() => setConfirm(null)} onClose={() => setConfirm(null)} aria-labelledby="action-title" aria-describedby="action-description"><p className="eyebrow">Local demo action</p><h2 id="action-title">{confirm?.name}</h2><p id="action-description">{confirm?.description}</p><div><button autoFocus className="button-secondary" onClick={() => setConfirm(null)}>Cancel</button><button className="button-primary" onClick={() => confirm && void runAction(confirm)}>Confirm action</button></div></dialog>
  </main>;
}

// Presentation order only; never used to claim or authorize delivery.
function rankEvents(events: RecoveryEvent[]) { return [...events].sort((a,b) => Number(terminalStates.has(a.state)) - Number(terminalStates.has(b.state)) || a.priority - b.priority || (a.deadlineAt ? Date.parse(a.deadlineAt) : Infinity) - (b.deadlineAt ? Date.parse(b.deadlineAt) : Infinity) || Date.parse(b.createdAt) - Date.parse(a.createdAt) || a.eventId.localeCompare(b.eventId)); }
function StateBadge({ state }: { state: string }) { return <span className={"state-badge state-" + state.toLowerCase().replaceAll("_", "-")}>{labels[state] ?? state.replaceAll("_", " ").toLowerCase()}</span>; }
function CircuitCard({ circuit, now }: { circuit: Circuit; now: number }) { return <article className={"target-card " + (circuit.state !== "CLOSED" ? "target-attention" : "")}><div><strong title={circuit.targetSystem}>{targetName(circuit.targetSystem)}</strong><StateBadge state={circuit.state} /></div><p>{circuit.state === "CLOSED" ? circuit.consecutiveFailures + " consecutive failures" : circuit.state === "HALF_OPEN" ? "One controlled probe admitted" : circuit.openUntil && Date.parse(circuit.openUntil) > now ? "Probe allowed " + relativeTime(circuit.openUntil, now) : "Cooldown elapsed · awaiting probe"}</p></article>; }

function EventDetail({ event, events, onSelect, now }: { event: RecoveryEvent; events: RecoveryEvent[]; onSelect: (id: string) => void; now: number }) {
  const audits = [...event.audits].sort((a,b) => Date.parse(b.occurredAt) - Date.parse(a.occurredAt));
  return <div className="event-detail">
    <p className="event-reason">{audits[0]?.reason ?? "No transition reason recorded."}</p>
    <dl><div><dt>Target</dt><dd>{targetName(event.targetSystem)}</dd></div><div><dt>Business priority</dt><dd>P{event.priority}</dd></div><div><dt>Received</dt><dd>{formatDate(event.createdAt)}</dd></div><div><dt>Deadline</dt><dd>{event.deadlineAt ? <>{formatDate(event.deadlineAt)}<small className="row-caption">{terminalStates.has(event.state) ? "Event finished" : deadlineText(event.deadlineAt, now)}</small></> : "Not specified"}</dd></div><div><dt>Attempts</dt><dd>{event.attemptCount}</dd></div><div><dt>Next eligibility</dt><dd>{terminalStates.has(event.state) ? "No further automatic action" : event.state === "BLOCKED" ? "After prerequisites complete" : formatDate(event.nextEligibleAt)}</dd></div></dl>
    <section className="detail-section"><h3>Prerequisites <span className="count-label">{event.prerequisiteEventIds.length}</span></h3>{event.prerequisiteEventIds.length ? <ul className="dependency-list">{event.prerequisiteEventIds.map(id => { const prerequisite = events.find(item => item.eventId === id); return <li key={id}><button onClick={() => onSelect(id)} disabled={!prerequisite}>{prerequisite ? messageName(prerequisite) : id}</button>{prerequisite && <StateBadge state={prerequisite.state} />}</li>; })}</ul> : <p>No prerequisites registered.</p>}</section>
    <section className="detail-section"><h3>Delivery attempts</h3>{event.attempts.length ? <div className="attempt-list">{[...event.attempts].reverse().map(attempt => <article key={attempt.attemptId}><div><strong>Attempt {attempt.attemptNumber}</strong><span>{attempt.httpStatus ? "HTTP " + attempt.httpStatus : "No HTTP result"} · {attempt.durationMillis === null ? "—" : formatDuration(attempt.durationMillis)}</span></div><p>{attempt.outcome.replaceAll("_", " ").toLowerCase()}{attempt.failureReason ? " · " + attempt.failureReason : ""}</p><time>{formatDate(attempt.startedAt)}</time></article>)}</div> : <p>No delivery attempted. Inspect the current state and prerequisites.</p>}</section>
    <section className="detail-section"><h3>Audit transitions</h3><ol className="timeline">{audits.map(audit => <li key={audit.auditId}><time dateTime={audit.occurredAt}>{formatDate(audit.occurredAt)}</time><strong>{labels[audit.toState]}</strong><p>{audit.reason}</p></li>)}</ol></section>
    <details className="technical-details"><summary>Technical evidence</summary><p>Event ID<br /><code>{event.eventId}</code></p><p>Correlation ID<br /><code>{event.correlationId}</code></p><p>Payload reference<br /><code>{event.payloadReference}</code></p><p>Target ID<br /><code>{event.targetSystem}</code></p><p>Lease owner: <code>{event.leaseOwner ?? "None"}</code><br />Lease expiry: {event.leaseExpiresAt ? formatDate(event.leaseExpiresAt) : "None"}</p>{event.attempts.map(attempt => <p key={attempt.attemptId}>Attempt {attempt.attemptNumber}: <code>{attempt.attemptId}</code><br />Worker: <code>{attempt.workerId}</code><br />Finished: {attempt.finishedAt ? formatDate(attempt.finishedAt) : "In progress"}</p>)}</details>
  </div>;
}
function formatDate(value: string) { return new Date(value).toLocaleString([], { dateStyle: "medium", timeStyle: "medium" }); }
function relativeTime(value: string, now: number) { const delta = Date.parse(value) - now; if (Math.abs(delta) < 60000) return delta > 0 ? "in <1m" : "now"; const minutes = Math.round(delta / 60000); return Math.abs(minutes) < 60 ? minutes >= 0 ? "in " + minutes + "m" : Math.abs(minutes) + "m ago" : formatDate(value); }
function deadlineText(value: string, now: number) { const minutes = Math.ceil((Date.parse(value) - now) / 60000); return minutes <= 0 ? "Deadline passed" : minutes < 60 ? "Due in " + minutes + "m" : "Due " + formatDate(value); }
function formatDuration(ms: number) { return ms < 1000 ? ms + " ms" : ms < 60000 ? (ms / 1000).toFixed(1) + " s" : (ms / 60000).toFixed(1) + " min"; }

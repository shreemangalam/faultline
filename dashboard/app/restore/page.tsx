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
type Statistics = {
  generatedAt: string; totalEvents: number; pendingEventCount: number; terminalFailureCount: number;
  eventCounts: Record<EventState, number>; totalAttempts: number;
  attemptCounts: Record<string, number>; p95SuccessfulRecoveryLatencyMillis: number | null;
};
type Circuit = { targetSystem: string; state: "CLOSED" | "OPEN" | "HALF_OPEN"; consecutiveFailures: number; openUntil: string | null; updatedAt: string };
type StreamState = "connecting" | "live" | "reconnecting";

const api = "/api/restore/v1";
const terminalStates = new Set<EventState>(["COMPLETED", "EXHAUSTED", "FAILED_FINAL", "FAILED_DEPENDENCY"]);
const filters: Array<"ALL" | EventState> = ["ALL", "ELIGIBLE", "BLOCKED", "IN_PROGRESS", "RETRY_SCHEDULED", "COMPLETED", "EXHAUSTED", "FAILED_FINAL", "FAILED_DEPENDENCY"];

export default function RestoreDashboard() {
  const [events, setEvents] = useState<RecoveryEvent[]>([]);
  const [statistics, setStatistics] = useState<Statistics | null>(null);
  const [circuits, setCircuits] = useState<Circuit[]>([]);
  const [selectedEventId, setSelectedEventId] = useState<string | null>(null);
  const [filter, setFilter] = useState<"ALL" | EventState>("ALL");
  const [streamState, setStreamState] = useState<StreamState>("connecting");
  const [loading, setLoading] = useState(true);
  const [action, setAction] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const refreshTimer = useRef<number | null>(null);

  const loadSnapshot = useCallback(async () => {
    try {
      const responses = await Promise.all([
        fetch(api + "/events", { cache: "no-store" }),
        fetch(api + "/statistics", { cache: "no-store" }),
        fetch(api + "/circuit-breakers", { cache: "no-store" })
      ]);
      if (responses.some((response) => !response.ok)) throw new Error("Restore API is unavailable. Start Restore on port 8082.");
      const eventBody = await responses[0].json() as RecoveryEvent[];
      const statisticsBody = await responses[1].json() as Statistics;
      const circuitBody = await responses[2].json() as Circuit[];
      setEvents(eventBody);
      setStatistics(statisticsBody);
      setCircuits(circuitBody);
      setSelectedEventId((current) => current && eventBody.some((item) => item.eventId === current) ? current : eventBody[0]?.eventId ?? null);
      setError(null);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Restore snapshot failed.");
    } finally {
      setLoading(false);
    }
  }, []);


  useEffect(() => {
    void loadSnapshot();
    const scheduleRefresh = () => {
      if (refreshTimer.current !== null) window.clearTimeout(refreshTimer.current);
      refreshTimer.current = window.setTimeout(() => void loadSnapshot(), 120);
    };
    const source = new EventSource(api + "/stream");
    source.addEventListener("stream-ready", () => {
      setStreamState("live");
      scheduleRefresh();
    });
    source.addEventListener("recovery-event", scheduleRefresh);
    source.onerror = () => setStreamState("reconnecting");
    return () => {
      if (refreshTimer.current !== null) window.clearTimeout(refreshTimer.current);
      source.close();
    };
  }, [loadSnapshot]);

  async function runAction(name: string, path: string, body?: object) {
    setAction(name);
    setError(null);
    setNotice(null);
    try {
      const response = await fetch(api + path, {
        method: "POST",
        headers: body ? { "Content-Type": "application/json" } : undefined,
        body: body ? JSON.stringify(body) : undefined
      });
      if (!response.ok) throw new Error("Restore rejected " + name + " (" + response.status + ").");
      const result = await response.json();
      const count = Array.isArray(result) ? result.length : result.eventCount;
      setNotice(name + " completed" + (typeof count === "number" ? " · " + count + " event" + (count === 1 ? "" : "s") : "") + ".");
      await loadSnapshot();
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Restore action failed.");
    } finally {
      setAction(null);
    }
  }

  const selectedEvent = useMemo(() => events.find((event) => event.eventId === selectedEventId) ?? null, [events, selectedEventId]);
  const visibleEvents = useMemo(() => events.filter((event) => filter === "ALL" || event.state === filter).slice(0, 40), [events, filter]);
  const healthyCircuits = circuits.filter((circuit) => circuit.state === "CLOSED").length;

  return <main id="main-content" className="app-shell restore-shell">

    <section className="page-heading restore-heading">
      <div><p className="eyebrow">RESTORE / RECOVERY CONTROL</p><h1>Durable recovery console</h1><p className="lede">Inspect dependency-aware work, target health, and the exact audit trail behind every recovery decision.</p></div>
      <div className={"stream-state " + streamState}><span className="pulse" />{streamState === "live" ? "LIVE UPDATES" : streamState.toUpperCase()}<strong>REST SNAPSHOT</strong></div>
    </section>

    {error && <p className="error" role="alert">{error}</p>}
    {notice && <p className="notice" role="status">{notice}</p>}
    {loading && <p className="loading">Loading the PostgreSQL-backed recovery snapshot…</p>}

    {statistics && <>
      <section className="metrics restore-metrics" aria-label="Recovery metrics">
        <Metric label="Pending queue" value={statistics.pendingEventCount} />
        <Metric label="In progress" value={statistics.eventCounts.IN_PROGRESS ?? 0} />
        <Metric label="Terminal failures" value={statistics.terminalFailureCount} warn={statistics.terminalFailureCount > 0} />
        <Metric label="Successful" value={statistics.eventCounts.COMPLETED ?? 0} />
        <Metric label="Healthy circuits" value={healthyCircuits + "/" + circuits.length} />
        <Metric label="P95 recovery" value={statistics.p95SuccessfulRecoveryLatencyMillis === null ? "—" : formatDuration(statistics.p95SuccessfulRecoveryLatencyMillis)} />
      </section>

      <section className="restore-grid">
        <div className="panel queue-panel">
          <div className="panel-heading"><div><p className="eyebrow">DURABLE QUEUE</p><h2>Recovery events</h2></div><span className="panel-code">{visibleEvents.length} / {events.length}</span></div>
          <div className="filter-row" role="group" aria-label="Filter recovery queue">
            {filters.map((state) => <button className={filter === state ? "active" : ""} key={state} type="button" onClick={() => setFilter(state)}>{state.replaceAll("_", " ")}</button>)}
          </div>
          <div className="table-wrap">
            <table className="queue-table"><thead><tr><th>Priority</th><th>Event</th><th>Target</th><th>State</th><th>Next action</th></tr></thead>
              <tbody>{visibleEvents.map((event) => <tr className={event.eventId === selectedEventId ? "selected" : ""} key={event.eventId} onClick={() => setSelectedEventId(event.eventId)}>
                <td><strong>P{event.priority}</strong></td>
                <td><button type="button" onClick={() => setSelectedEventId(event.eventId)}><span>{event.correlationId}</span><small>{shortId(event.eventId)}</small></button></td>
                <td>{event.targetSystem}</td>
                <td><StateBadge state={event.state} /></td>
                <td>{event.state === "BLOCKED" ? event.audits.at(-1)?.reason ?? "Waiting for prerequisite" : terminalStates.has(event.state) ? "No automatic action" : relativeTime(event.nextEligibleAt)}</td>
              </tr>)}</tbody>
            </table>
          </div>
          {visibleEvents.length === 0 && <p className="empty-state">No events match this state.</p>}
        </div>

        <aside className="panel scenario-panel">
          <div className="panel-heading"><div><p className="eyebrow">CONTROLLED DEMOS</p><h2>Scenario controls</h2></div><span className="panel-code">MANUAL</span></div>
          <p>Load fictional recovery data or execute one bounded worker batch. Automatic delivery remains disabled.</p>
          <div className="scenario-actions">
            <ActionButton label="Dependency cascade" busy={action === "Dependency cascade"} onClick={() => runAction("Dependency cascade", "/scenarios/dependency-cascade")} />
            <ActionButton label="Priority & deadline" busy={action === "Priority & deadline"} onClick={() => runAction("Priority & deadline", "/scenarios/priority-deadline")} />
            <ActionButton label="Terminal failures" busy={action === "Terminal failures"} onClick={() => runAction("Terminal failures", "/scenarios/terminal-failures")} />
            <ActionButton label="100-event outage" busy={action === "100-event outage"} onClick={() => runAction("100-event outage", "/scenarios/thundering-herd", { eventCount: 100 })} />
            <ActionButton primary label="Run next batch" busy={action === "Run next batch"} onClick={() => runAction("Run next batch", "/worker/run-once", { workerId: "dashboard-worker", maxEvents: 5, leaseSeconds: 60 })} />
          </div>
          <small className="control-warning">Actions write to the local Restore database and use production intake/worker rules.</small>
        </aside>
      </section>

      <section className="restore-lower">
        <div className="panel circuit-panel">
          <div className="panel-heading"><div><p className="eyebrow">TARGET HEALTH</p><h2>Circuit breakers</h2></div><span className="panel-code">{circuits.length} TARGETS</span></div>
          <div className="circuit-list">{circuits.map((circuit) => <article key={circuit.targetSystem}><div><StateBadge state={circuit.state} /><strong>{circuit.targetSystem}</strong></div><span>{circuit.consecutiveFailures} consecutive failure{circuit.consecutiveFailures === 1 ? "" : "s"}</span><small>{circuit.openUntil ? "Open until " + formatDate(circuit.openUntil) : "Updated " + relativeTime(circuit.updatedAt)}</small></article>)}</div>
          {circuits.length === 0 && <p className="empty-state">No target circuits have been created yet.</p>}
        </div>
        <div className="panel timeline-panel">
          <div className="panel-heading"><div><p className="eyebrow">EVENT EVIDENCE</p><h2>{selectedEvent ? selectedEvent.correlationId : "Select an event"}</h2></div>{selectedEvent && <StateBadge state={selectedEvent.state} />}</div>
          {selectedEvent ? <EventTimeline event={selectedEvent} /> : <p className="empty-state">Choose a queue row to inspect its persisted transitions and attempts.</p>}
        </div>
      </section>
    </>}
  </main>;
}

function Metric({ label, value, warn = false }: { label: string; value: string | number; warn?: boolean }) {
  return <div className={"metric " + (warn ? "metric-warn" : "")}><span>{label}</span><strong>{value}</strong></div>;
}

function StateBadge({ state }: { state: string }) {
  return <span className={"state-badge state-" + state.toLowerCase().replaceAll("_", "-")}>{state.replaceAll("_", " ")}</span>;
}

function ActionButton({ label, busy, primary = false, onClick }: { label: string; busy: boolean; primary?: boolean; onClick: () => void }) {
  return <button className={primary ? "primary" : ""} type="button" disabled={busy} onClick={onClick}>{busy ? "Working…" : label}<span>↗</span></button>;
}

function EventTimeline({ event }: { event: RecoveryEvent }) {
  const items = [
    ...event.audits.map((audit) => ({ id: audit.auditId, at: audit.occurredAt, label: audit.toState.replaceAll("_", " "), detail: audit.reason })),
    ...event.attempts.map((attempt) => ({ id: attempt.attemptId, at: attempt.finishedAt ?? attempt.startedAt, label: "ATTEMPT " + attempt.attemptNumber + " · " + attempt.outcome.replaceAll("_", " "), detail: attempt.failureReason ?? (attempt.httpStatus ? "HTTP " + attempt.httpStatus : "Worker " + attempt.workerId) }))
  ].sort((a, b) => Date.parse(b.at) - Date.parse(a.at));

  return <div className="event-detail">
    <dl><div><dt>Event</dt><dd>{shortId(event.eventId)}</dd></div><div><dt>Target</dt><dd>{event.targetSystem}</dd></div><div><dt>Attempts</dt><dd>{event.attemptCount}</dd></div><div><dt>Dependencies</dt><dd>{event.prerequisiteEventIds.length}</dd></div></dl>
    <ol className="timeline">{items.map((item) => <li key={item.id}><time>{formatDate(item.at)}</time><div><strong>{item.label}</strong><p>{item.detail}</p></div></li>)}</ol>
  </div>;
}

function shortId(value: string) { return value.slice(0, 8); }
function formatDate(value: string) { return new Date(value).toLocaleString([], { dateStyle: "medium", timeStyle: "short" }); }
function relativeTime(value: string) {
  const delta = Date.parse(value) - Date.now();
  if (Math.abs(delta) < 1000) return "now";
  const minutes = Math.round(delta / 60000);
  if (Math.abs(minutes) < 60) return minutes >= 0 ? "in " + minutes + "m" : Math.abs(minutes) + "m ago";
  return formatDate(value);
}
function formatDuration(milliseconds: number) {
  return milliseconds < 1000 ? milliseconds + " ms" : (milliseconds / 1000).toFixed(milliseconds < 10000 ? 1 : 0) + " s";
}

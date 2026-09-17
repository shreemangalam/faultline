"use client";

import Link from "next/link";
import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import { LandscapeGraph } from "../components/landscape-graph";

type SystemNode = { systemId: string; label: string; criticality: number; health: string };
type Integration = { integrationId: string; sourceSystemId: string; targetSystemId: string; protocol: string; dataDomain: string; slaClass: string };
type HealthMap = { systems: SystemNode[]; integrations: Integration[] };
type Report = {
  reportId: string; createdAt: string; landscapeReference: string;
  incident: { systemId: string; degradationType: string };
  affectedSystems: Array<{ systemId: string; label: string; impactRing: number; classification: string }>;
  priorities: Array<{ systemId: string; label: string; impactRing: number; score: number; severity: string }>;
  recovery: { recoverySequence: string[]; cycleWarning: boolean; unresolvedSystemIds: string[] };
  warnings: string[];
};
const api = "/api/survey/v1";
const human = (value: string) => value.replaceAll("_", " ").toLowerCase();

export default function SurveyDashboard() {
  const [healthMap, setHealthMap] = useState<HealthMap | null>(null);
  const [warnings, setWarnings] = useState<string[]>([]);
  const [selectedSystemId, setSelectedSystemId] = useState("SALESFORCE");
  const [report, setReport] = useState<Report | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const responses = await Promise.all([fetch(api + "/landscape/health-map"), fetch(api + "/landscape/warnings")]);
      if (responses.some(response => !response.ok)) throw new Error("The landscape could not be loaded. Check the Survey service and try again.");
      const map = await responses[0].json() as HealthMap;
      const warningBody = await responses[1].json() as { warnings: string[] };
      setHealthMap(map);
      setWarnings(warningBody.warnings);
      setSelectedSystemId(current => map.systems.some(system => system.systemId === current) ? current : map.systems[0]?.systemId ?? "");
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Survey request failed."); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { void load(); }, [load]);

  async function createReport(event: FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const response = await fetch(api + "/analysis/impact", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ systemId: selectedSystemId, degradationType: "COMPLETE_OUTAGE" }) });
      if (!response.ok) throw new Error("The impact report could not be created. Your existing report is still available.");
      setReport(await response.json() as Report);
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Survey request failed."); }
    finally { setSubmitting(false); }
  }

  const selected = healthMap?.systems.find(system => system.systemId === selectedSystemId);
  const paths = useMemo(() => healthMap?.integrations.filter(edge => edge.sourceSystemId === selectedSystemId || edge.targetSystemId === selectedSystemId) ?? [], [healthMap, selectedSystemId]);
  const label = (id: string) => healthMap?.systems.find(system => system.systemId === id)?.label ?? id;

  return <main id="main-content" className="app-shell survey-shell">
    <section className="page-heading">
      <div><p className="eyebrow">Survey / Landscape intelligence</p><h1>Understand the impact.</h1><p className="lede">Explore your integration landscape, trace exposure, and establish a safe recovery order.</p></div>
      <div className="landscape-state"><span className="pulse" />{error ? "Service needs attention" : loading ? "Connecting to Survey" : "Landscape snapshot loaded"}</div>
    </section>
    {error && <div className="error" role="alert">{error} <button className="button-secondary" onClick={() => void load()}>Retry connection</button></div>}
    {loading && !healthMap && <div className="loading" role="status">Loading systems and integration paths…</div>}
    {healthMap && <>
      <div className="landscape-toolbar"><div><strong>{healthMap.systems.length}</strong> systems <span>/</span> <strong>{healthMap.integrations.length}</strong> integration paths</div><span>{warnings.length ? warnings.length + " topology warning" + (warnings.length === 1 ? "" : "s") : "No topology warnings"} · Select a system to focus its connections</span></div>
      <section className="workspace">
        <div className="panel graph-panel">
          <div className="panel-heading"><div><h2>Integration landscape</h2><p className="panel-description">From business sources to downstream delivery</p></div><div className="graph-legend"><span><i className="legend-dot" />Selected system</span><span><i className="legend-arrow" />Data direction →</span></div></div>
          <div className="graph-scroll" tabIndex={0} role="region" aria-label="Scrollable landscape topology"><LandscapeGraph systems={healthMap.systems} integrations={healthMap.integrations} selectedSystemId={selectedSystemId} onSelect={setSelectedSystemId} /></div>
          <div className="graph-caption"><span>Focus: <strong>{selected?.label ?? "No system selected"}</strong></span><span>Solid connections show immediate upstream and downstream paths.</span></div>
        </div>
        <aside className="panel control-panel">
          <div className="panel-heading"><h2>System focus</h2><span className="panel-code">Inspect & analyze</span></div>
          <div className="selected-system"><span>Selected system</span><strong>{selected?.label ?? "No systems"}</strong><div className="system-facts"><span>Health: {human(selected?.health ?? "unknown")}</span><span>Criticality {selected?.criticality ?? "—"}/10</span></div></div>
          <form onSubmit={createReport}><label htmlFor="system">Failure origin</label><select id="system" value={selectedSystemId} onChange={event => setSelectedSystemId(event.target.value)}>{healthMap.systems.map(system => <option key={system.systemId} value={system.systemId}>{system.label}</option>)}</select><button disabled={submitting || !selectedSystemId}>{submitting ? "Analyzing dependencies…" : "Analyze complete outage"}<span aria-hidden="true">→</span></button></form>
          <p className="control-footnote">Creates an incident report. Does not change system health or replay messages.</p>
          {warnings.length > 0 && <details className="topology-warning"><summary>{warnings.length} topology warning{warnings.length === 1 ? "" : "s"}</summary>{warnings.map(warning => <p key={warning}>{warning}</p>)}</details>}
        </aside>
      </section>

      {report ? <section className="incident-workspace panel" aria-label="Incident report">
        <div className="incident-heading"><div><p className="eyebrow">Incident assessment / Complete outage</p><h2>{label(report.incident.systemId)} outage</h2><p>{report.affectedSystems.length} exposed systems · {report.priorities.filter(p => p.severity === "CRITICAL").length} critical priorities · {new Date(report.createdAt).toLocaleString()}</p></div><Link className="button-secondary" href="/restore">Inspect recovery queue →</Link></div>
        {report.incident.systemId !== selectedSystemId && <p className="report-context">This report covers {label(report.incident.systemId)}. Analyze {label(selectedSystemId)} to create a new assessment.</p>}
        <div className="incident-columns">
          <div><div className="panel-heading"><h3>Business exposure</h3><span className="panel-code">Backend-ranked priorities</span></div>
            <div className="table-wrap"><table className="impact-table"><thead><tr><th>System / exposure</th><th>Severity</th><th>Score</th></tr></thead><tbody>{report.priorities.map(priority => {
              const impact = report.affectedSystems.find(system => system.systemId === priority.systemId);
              return <tr key={priority.systemId}><td><strong>{priority.label}</strong><small>{impact ? human(impact.classification) : "Affected"} · {priority.impactRing === 1 ? "Direct impact" : "Cascade · ring " + priority.impactRing}</small></td><td><span className={"state-badge severity-" + priority.severity.toLowerCase()}>{human(priority.severity)}</span></td><td>{priority.score}</td></tr>;
            })}</tbody></table></div>
            {report.affectedSystems.length === 0 && <p className="empty-state">No downstream systems are exposed in this landscape snapshot.</p>}
          </div>
          <div className="recovery"><div className="panel-heading"><h3>Dependency-safe recovery</h3><span className="state-badge">{report.recovery.cycleWarning ? "Partial sequence" : "Safe sequence"}</span></div>
            <ol>{report.recovery.recoverySequence.map((id, index) => <li key={id}><i>{String(index + 1).padStart(2, "0")}</i><span>{label(id)}</span></li>)}</ol>
            {report.recovery.cycleWarning && <div className="warning"><strong>Manual review required</strong><p>A cycle prevents a complete recovery order. Unresolved: {report.recovery.unresolvedSystemIds.map(label).join(", ")}.</p></div>}
            {report.warnings.map(warning => <p className="report-warning" key={warning}>{warning}</p>)}
          </div>
        </div>
        <details className="technical-details"><summary>Report evidence</summary><p>Report <code>{report.reportId}</code></p><p>Landscape <code>{report.landscapeReference}</code></p><p>Impact and recovery sequence are provided by Survey. This assessment is independent of Restore queue state.</p></details>
      </section> : <div className="analysis-prompt"><span className="step-number">01</span><div><strong>Start with a failure origin.</strong><p>Select a system, then analyze its outage to see exposure, priorities, and recovery steps here.</p></div></div>}

      <section className="panel connection-panel"><div className="panel-heading"><div><h2>{selected?.label ?? "System"} connections</h2><p className="panel-description">Protocol, business domain and SLA from the active landscape</p></div><span className="panel-code">{paths.length} paths</span></div>
        <div className="connection-grid">{paths.map(edge => <article key={edge.integrationId}><p><span>{edge.targetSystemId === selectedSystemId ? "Upstream" : "Downstream"}</span><strong>{label(edge.sourceSystemId)} → {label(edge.targetSystemId)}</strong></p><div><b>{edge.protocol}</b><span>{human(edge.dataDomain)}</span><span>{human(edge.slaClass)}</span></div></article>)}</div>
        {paths.length === 0 && <p className="empty-state">This system has no registered connections.</p>}
      </section>
    </>}
  </main>;
}

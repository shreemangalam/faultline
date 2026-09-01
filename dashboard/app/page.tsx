"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
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

export default function SurveyDashboard() {
  const [healthMap, setHealthMap] = useState<HealthMap | null>(null);
  const [warnings, setWarnings] = useState<string[]>([]);
  const [selectedSystemId, setSelectedSystemId] = useState("SAP_ECC");
  const [report, setReport] = useState<Report | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    Promise.all([fetch(`${api}/landscape/health-map`), fetch(`${api}/landscape/warnings`)])
      .then(async ([mapResponse, warningResponse]) => {
        if (!mapResponse.ok || !warningResponse.ok) throw new Error("Survey API is unavailable. Start Survey on port 8081.");
        const map = await mapResponse.json() as HealthMap;
        const warningBody = await warningResponse.json() as { warnings: string[] };
        setHealthMap(map); setWarnings(warningBody.warnings);
        if (!map.systems.some((system) => system.systemId === selectedSystemId)) setSelectedSystemId(map.systems[0]?.systemId ?? "");
      })
      .catch((reason: Error) => setError(reason.message))
      .finally(() => setLoading(false));
  }, []);

  async function createReport(event: FormEvent) {
    event.preventDefault(); setSubmitting(true); setError(null);
    try {
      const response = await fetch(`${api}/analysis/impact`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ systemId: selectedSystemId, degradationType: "COMPLETE_OUTAGE" }) });
      if (!response.ok) throw new Error(`Survey could not analyze ${selectedSystemId}.`);
      setReport(await response.json() as Report);
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Unexpected request failure."); }
    finally { setSubmitting(false); }
  }

  const criticalCount = useMemo(() => report?.priorities.filter((priority) => priority.severity === "CRITICAL").length ?? 0, [report]);
  return <main>
    <header><p className="eyebrow">FAULTLINE / SURVEY</p><h1>Integration landscape impact analysis</h1><p className="lede">Select a failed system to create a durable, explainable incident report.</p></header>
    {error && <p className="error" role="alert">{error}</p>}
    {loading && <p className="loading">Loading the active landscape…</p>}
    {healthMap && <>
      <section className="metrics" aria-label="Landscape metrics"><Metric label="Systems" value={healthMap.systems.length} /><Metric label="Flows" value={healthMap.integrations.length} /><Metric label="Cycle warnings" value={warnings.length} /><Metric label="Critical impacts" value={criticalCount} /></section>
      <section className="workspace"><div className="panel graph-panel"><div className="panel-heading"><div><p className="eyebrow">ACTIVE TOPOLOGY</p><h2>Landscape health map</h2></div><span className="legend">Click a node to select it</span></div><LandscapeGraph systems={healthMap.systems} integrations={healthMap.integrations} selectedSystemId={selectedSystemId} onSelect={setSelectedSystemId} /></div>
      <aside className="panel control-panel"><p className="eyebrow">INCIDENT</p><h2>Create impact report</h2><form onSubmit={createReport}><label htmlFor="system">Failed system</label><select id="system" value={selectedSystemId} onChange={(event) => setSelectedSystemId(event.target.value)}>{healthMap.systems.map((system) => <option key={system.systemId} value={system.systemId}>{system.label} ({system.systemId})</option>)}</select><button disabled={submitting || !selectedSystemId}>{submitting ? "Analyzing…" : "Analyze complete outage"}</button></form>{warnings.length > 0 && <div className="warning"><strong>Topology warning</strong><p>{warnings[0]}</p></div>}</aside></section>
      {report && <ReportPanel report={report} />}
    </>}
  </main>;
}
function Metric({ label, value }: { label: string; value: number }) { return <div className="metric"><span>{label}</span><strong>{value}</strong></div>; }
function ReportPanel({ report }: { report: Report }) { return <section className="report"><div className="panel report-summary"><p className="eyebrow">PERSISTED REPORT</p><h2>{report.incident.systemId} complete outage</h2><p className="muted">{new Date(report.createdAt).toLocaleString()} · {report.reportId}</p><p>{report.affectedSystems.length} affected systems on {report.landscapeReference}.</p></div><div className="panel"><h3>Impact classification</h3><ul className="rows">{report.affectedSystems.map((system) => <li key={system.systemId}><span>{system.label}<small>Ring {system.impactRing}</small></span><b>{system.classification}</b></li>)}</ul></div><div className="panel"><h3>Recovery guidance</h3><p>{report.recovery.recoverySequence.join(" → ") || "No automatic sequence is safe."}</p>{report.warnings.map((warning) => <p className="warning" key={warning}>{warning}</p>)}</div></section>; }
"use client";

import Link from "next/link";
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

type Theme = "dark" | "light";
const api = "/api/survey/v1";

export default function SurveyDashboard() {
  const [healthMap, setHealthMap] = useState<HealthMap | null>(null);
  const [warnings, setWarnings] = useState<string[]>([]);
  const [selectedSystemId, setSelectedSystemId] = useState("SAP_ECC");
  const [report, setReport] = useState<Report | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [theme, setTheme] = useState<Theme>("dark");

  useEffect(() => {
    const savedTheme = window.localStorage.getItem("faultline-theme");
    if (savedTheme === "light" || savedTheme === "dark") setTheme(savedTheme);
    else if (window.matchMedia("(prefers-color-scheme: light)").matches) setTheme("light");
  }, []);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    window.localStorage.setItem("faultline-theme", theme);
  }, [theme]);

  useEffect(() => {
    Promise.all([fetch(`${api}/landscape/health-map`), fetch(`${api}/landscape/warnings`)])
      .then(async ([mapResponse, warningResponse]) => {
        if (!mapResponse.ok || !warningResponse.ok) throw new Error("Survey API is unavailable. Start Survey on port 8081.");
        const map = await mapResponse.json() as HealthMap;
        const warningBody = await warningResponse.json() as { warnings: string[] };
        setHealthMap(map);
        setWarnings(warningBody.warnings);
        if (!map.systems.some((system) => system.systemId === selectedSystemId)) setSelectedSystemId(map.systems[0]?.systemId ?? "");
      })
      .catch((reason: Error) => setError(reason.message))
      .finally(() => setLoading(false));
  }, []);

  async function createReport(event: FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const response = await fetch(`${api}/analysis/impact`, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ systemId: selectedSystemId, degradationType: "COMPLETE_OUTAGE" }) });
      if (!response.ok) throw new Error(`Survey could not analyze ${selectedSystemId}.`);
      setReport(await response.json() as Report);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Unexpected request failure.");
    } finally {
      setSubmitting(false);
    }
  }

  const selectedSystem = useMemo(() => healthMap?.systems.find((system) => system.systemId === selectedSystemId), [healthMap, selectedSystemId]);
  const criticalCount = useMemo(() => report?.priorities.filter((priority) => priority.severity === "CRITICAL").length ?? 0, [report]);
  const themeLabel = theme === "dark" ? "Use light theme" : "Use dark theme";

  return <main className="app-shell">
    <header className="topbar">
      <div className="brand"><span className="brand-mark">F</span><span>FAULTLINE</span></div><nav className="module-nav" aria-label="Faultline modules"><Link className="active" href="/">Survey</Link><Link href="/restore">Restore</Link></nav>
      <div className="topbar-actions"><span className="environment"><i />LOCAL LANDSCAPE</span><button className="theme-toggle" type="button" onClick={() => setTheme(theme === "dark" ? "light" : "dark")} aria-label={themeLabel} title={themeLabel}>{theme === "dark" ? "☼" : "◐"}</button></div>
    </header>
    <section className="page-heading"><div><p className="eyebrow">SURVEY / ACTIVE LANDSCAPE</p><h1>Integration impact analysis</h1><p className="lede">Trace downstream exposure and establish a safe recovery order from the active landscape.</p></div><div className="landscape-state"><span className="pulse" />ACTIVE TOPOLOGY <strong>{healthMap ? "ONLINE" : "CONNECTING"}</strong></div></section>
    {error && <p className="error" role="alert">{error}</p>}
    {loading && <p className="loading">Connecting to the active landscape…</p>}
    {healthMap && <>
      <section className="metrics" aria-label="Landscape metrics"><Metric label="Systems observed" value={healthMap.systems.length} /><Metric label="Integration paths" value={healthMap.integrations.length} /><Metric label="Topology exceptions" value={warnings.length} warn={warnings.length > 0} /><Metric label="Critical impact" value={criticalCount} warn={criticalCount > 0} /></section>
      <section className="workspace">
        <div className="panel graph-panel"><div className="panel-heading"><div><p className="eyebrow">LIVE DEPENDENCY MAP</p><h2>Landscape topology</h2></div><div className="graph-legend"><span><i className="legend-dot" />System</span><span><i className="legend-arrow" />Data direction</span><span>Node size = criticality</span></div></div><LandscapeGraph systems={healthMap.systems} integrations={healthMap.integrations} selectedSystemId={selectedSystemId} onSelect={setSelectedSystemId} /></div>
        <aside className="panel control-panel"><div className="panel-heading"><div><p className="eyebrow">FAILURE SIMULATION</p><h2>Incident console</h2></div><span className="panel-code">01</span></div><div className="selected-system"><span>SELECTED SYSTEM</span><strong>{selectedSystem?.label ?? "—"}</strong><code>{selectedSystemId}</code><small>Criticality {selectedSystem?.criticality ?? "—"} / 10</small></div><form onSubmit={createReport}><label htmlFor="system">Failure origin</label><select id="system" value={selectedSystemId} onChange={(event) => setSelectedSystemId(event.target.value)}>{healthMap.systems.map((system) => <option key={system.systemId} value={system.systemId}>{system.label} · {system.systemId}</option>)}</select><button disabled={submitting || !selectedSystemId}>{submitting ? "Tracing dependencies…" : "Run outage analysis"}<span>↗</span></button></form>{warnings.length > 0 && <div className="warning"><div><span className="warning-indicator">!</span><strong>Topology exception</strong></div><p title={warnings[0]}>{warnings[0]}</p></div>}<p className="control-footnote">Creates an immutable report against the active landscape snapshot.</p></aside>
      </section>
      {report && <ReportPanel report={report} />}
    </>}
  </main>;
}

function Metric({ label, value, warn = false }: { label: string; value: number; warn?: boolean }) { return <div className={`metric ${warn ? "metric-warn" : ""}`}><span>{label}</span><strong>{String(value).padStart(2, "0")}</strong></div>; }

function ReportPanel({ report }: { report: Report }) { return <section className="report"><div className="panel report-summary"><p className="eyebrow">IMMUTABLE INCIDENT REPORT</p><h2>{report.incident.systemId} <span>/ COMPLETE OUTAGE</span></h2><p className="report-meta">{new Date(report.createdAt).toLocaleString()} · {report.reportId}</p><div className="report-total"><strong>{String(report.affectedSystems.length).padStart(2, "0")}</strong><span>systems exposed<br />on {report.landscapeReference}</span></div></div><div className="panel report-list"><div className="panel-heading"><h3>Impact classification</h3><span>{report.affectedSystems.length} downstream</span></div><ul className="rows">{report.affectedSystems.map((system) => <li key={system.systemId}><span><strong>{system.label}</strong><small>Impact ring {system.impactRing}</small></span><b>{system.classification}</b></li>)}</ul></div><div className="panel recovery"><div className="panel-heading"><h3>Recovery sequence</h3><span>{report.recovery.cycleWarning ? "Manual review" : "Safe order"}</span></div><ol>{report.recovery.recoverySequence.map((systemId, index) => <li key={systemId}><i>{String(index + 1).padStart(2, "0")}</i>{systemId}</li>)}</ol>{report.warnings.map((warning) => <p className="report-warning" key={warning}>{warning}</p>)}</div></section>; }
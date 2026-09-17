"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

type Theme = "dark" | "light";

const lifecycle = [
  ["RECEIVED / ELIGIBLE", "Restore validates and persists each event before it can be claimed."],
  ["BLOCKED", "At least one prerequisite is unfinished. The event cannot bypass dependency order."],
  ["IN PROGRESS", "A worker owns a time-limited lease and is making the downstream attempt."],
  ["RETRY SCHEDULED", "A transient failure is waiting for bounded exponential backoff and jitter."],
  ["COMPLETED", "The target accepted the event; eligible dependants can now be released."],
  ["TERMINAL", "Exhausted, final, or dependency failures stop automatic retry and remain queryable."]
];

export default function HelpPage() {
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

  const themeLabel = theme === "dark" ? "Use light theme" : "Use dark theme";

  return <main className="app-shell help-shell">
    <header className="topbar">
      <div className="brand"><span className="brand-mark">F</span><span>FAULTLINE</span></div>
      <nav className="module-nav" aria-label="Faultline modules"><Link href="/">Survey</Link><Link href="/restore">Restore</Link><Link className="active" href="/help">Help</Link></nav>
      <div className="topbar-actions"><span className="environment"><i />LOCAL CONTROL PLANE</span><button className="theme-toggle" type="button" onClick={() => setTheme(theme === "dark" ? "light" : "dark")} aria-label={themeLabel} title={themeLabel}>{theme === "dark" ? "☼" : "◐"}</button></div>
    </header>

    <section className="page-heading">
      <div><p className="eyebrow">HELP / HOW IT WORKS</p><h1>From outage to safe recovery</h1><p className="lede">Faultline separates impact analysis from message recovery, then keeps every decision visible in backend-owned evidence.</p></div>
      <div className="landscape-state"><span className="pulse" />LOCAL DEMO <strong>NO REAL SYSTEMS</strong></div>
    </section>

    <section className="how-flow" aria-label="Faultline workflow">
      <article><span>01</span><div><p className="eyebrow">SURVEY</p><h2>Declare an outage</h2><p>Select a fictional system and create an immutable impact report against the active landscape.</p></div></article>
      <article><span>02</span><div><p className="eyebrow">ANALYZE</p><h2>Read the blast radius</h2><p>Follow downstream impact rings, explainable priority scores, cycle warnings, and the safe recovery sequence.</p></div></article>
      <article><span>03</span><div><p className="eyebrow">RESTORE</p><h2>Inspect durable work</h2><p>Use queue state, prerequisites, circuits, attempts, and audit history to understand what can run and why.</p></div></article>
      <article><span>04</span><div><p className="eyebrow">RECOVER</p><h2>Run one bounded batch</h2><p>Load a scenario, verify target health, then explicitly run the next batch. Automatic delivery is off by default.</p></div></article>
    </section>

    <section className="help-grid">
      <div className="panel help-panel">
        <div className="panel-heading"><div><p className="eyebrow">STATE GUIDE</p><h2>Recovery lifecycle</h2></div><span className="panel-code">POSTGRESQL</span></div>
        <dl className="status-guide">{lifecycle.map(([state, description]) => <div key={state}><dt>{state}</dt><dd>{description}</dd></div>)}</dl>
      </div>

      <div className="panel help-panel">
        <div className="panel-heading"><div><p className="eyebrow">SAFE DEMO</p><h2>Operator checklist</h2></div><span className="panel-code">MANUAL</span></div>
        <ol className="operator-checklist">
          <li><strong>Start with the snapshot.</strong><span>Queue, statistics, circuits, and event evidence come from PostgreSQL-backed REST.</span></li>
          <li><strong>Choose a controlled scenario.</strong><span>Scenario buttons create fictional local records using the same intake rules as ordinary events.</span></li>
          <li><strong>Review before execution.</strong><span>Blocked reasons, priorities, deadlines, and circuit state explain admission decisions.</span></li>
          <li><strong>Run one bounded batch.</strong><span>The dashboard action claims at most five events. It does not enable the scheduled worker.</span></li>
          <li><strong>Watch, then verify.</strong><span>SSE signals a change; the browser refreshes the authoritative REST snapshot and timeline.</span></li>
        </ol>
      </div>

      <div className="panel help-panel help-boundaries">
        <div className="panel-heading"><div><p className="eyebrow">DESIGN BOUNDARIES</p><h2>What Faultline proves</h2></div></div>
        <ul>
          <li><strong>Survey owns graph reasoning.</strong> The browser never recomputes impact or recovery order.</li>
          <li><strong>Restore owns retry policy.</strong> Dependencies, leases, backoff, terminal states, and circuits remain Java rules.</li>
          <li><strong>PostgreSQL owns durable truth.</strong> SSE can drop invalidations; reconnects always rebuild from REST.</li>
          <li><strong>The simulator owns no real data.</strong> Targets, payload references, and business flows are fictional and local.</li>
        </ul>
        <div className="help-links"><Link href="/">Open Survey <span>↗</span></Link><Link href="/restore">Open Restore <span>↗</span></Link></div>
      </div>
    </section>
  </main>;
}

import Link from "next/link";

const lifecycle = [
  ["Ready", "Eligible for worker admission. Target circuits and worker ranking still apply."],
  ["Blocked", "Waiting for prerequisites. The event becomes eligible only after every prerequisite succeeds."],
  ["In progress", "A worker owns a time-limited lease. The attempt may be executing or waiting for a result."],
  ["Retry waiting", "A temporary failure is waiting for backoff and jitter before another attempt."],
  ["Completed", "The target accepted the message. Restore releases dependants whose prerequisites are now complete."],
  ["Final failure", "The target rejected the message permanently. The message is retained as terminal evidence."],
  ["Dependency failed", "A prerequisite cannot complete, so the dependant stops instead of retrying forever."],
  ["Exhausted", "The attempt limit was reached. Automatic retries stop; the event remains queryable."]
];

export default function HelpPage() {
  return <main id="main-content" className="app-shell help-shell">
    <section className="page-heading"><div><p className="eyebrow">Operator guide / Start here</p><h1>From incident to recovery.</h1><p className="lede">A short walkthrough of impact analysis, message dependencies and controlled recovery.</p></div><span className="guide-duration">5 minute walkthrough</span></section>

    <section className="guide-intro panel"><div><p className="eyebrow">The situation</p><h2>Orders are waiting on Salesforce.</h2><p>In the curated demo, Salesforce returns HTTP 503. Its circuit opens, order messages wait for retry, and downstream inventory, payment and shipment work stays blocked. Your job is to understand the exposure, inspect the evidence, then recover in bounded steps.</p></div><div className="guide-start"><span>Fictional data. Real recovery behavior.</span><Link className="button-primary" href="/">Begin in Survey →</Link><Link href="/restore">Go straight to the recovery queue</Link></div></section>

    <section className="how-flow" aria-label="Operator workflow">
      <article><span>01</span><div><p className="eyebrow">Survey</p><h2>Locate the failure</h2><p>Select a system to focus its upstream and downstream connections.</p></div></article>
      <article><span>02</span><div><p className="eyebrow">Incident report</p><h2>Assess the exposure</h2><p>Read severity, priority, recovery order and any cycle warnings.</p></div></article>
      <article><span>03</span><div><p className="eyebrow">Restore</p><h2>Inspect the messages</h2><p>Follow orders, prerequisites and the evidence behind each state.</p></div></article>
      <article><span>04</span><div><p className="eyebrow">Bounded recovery</p><h2>Recover and verify</h2><p>Restore the simulated target, execute a batch and inspect what changed.</p></div></article>
    </section>

    <section className="guide-workspace">
      <div className="panel help-panel"><div className="panel-heading"><div><p className="eyebrow">Hands-on walkthrough</p><h2>Your first five minutes</h2></div></div>
        <ol className="operator-checklist">
          <li><strong>Trace the Salesforce outage <small>0:00–1:00</small></strong><span>Open Survey, select Salesforce and choose Analyze complete outage. Read the exposed systems and their backend-ranked severity. Try SAP ECC afterward to see a larger blast radius and the deliberate legacy cycle.</span></li>
          <li><strong>Read the incident assessment <small>1:00–2:00</small></strong><span>Inspect the recovery sequence and any manual-review warning. A cycle produces a safe partial order, not an invented full sequence. Reports are immutable snapshots.</span></li>
          <li><strong>Find order ORD-2026-10482 <small>2:00–3:00</small></strong><span>In the isolated demo’s Restore queue, search this reference. Inspect the CPI order message’s 503 attempt and the Salesforce circuit. Select an inventory or payment message and follow its prerequisite link.</span></li>
          <li><strong>Recover in bounded steps <small>3:00–4:00</small></strong><span>Expand Demo tools. Confirm Recover Salesforce endpoint, then Run next batch. If cooldown has elapsed, one probe can close the circuit. Run subsequent batches to release the remaining order chain.</span></li>
          <li><strong>Verify the evidence <small>4:00–5:00</small></strong><span>Inspect new HTTP results and audit transitions. Search ORD-2026-10479 for a successful WMS retry, then CUS-2026-2081 for final and dependency failures. Compare the updated queue counts and recovery latency.</span></li>
        </ol>
      </div>
      <aside className="guide-reference">
        <div className="panel help-panel"><p className="eyebrow">Before you begin</p><h2>Choose the right dataset.</h2><p>The curated walkthrough runs on the isolated operations-demo stack at port 3002. It uses a separate PostgreSQL volume. The preserved development data remains available on its own dashboard.</p><p>On an empty demo database, expand Demo tools and confirm Load operations demo. Allow several seconds for real HTTP attempts and retry backoff. Loading a nonempty database is rejected.</p><div className="warning"><strong>Every action is real, local work.</strong><p>Scenario loaders add records. Manual batches deliver eligible messages to fictional targets. They do not enable the scheduled worker.</p></div><Link className="guide-link" href="/restore">Open recovery operations →</Link></div>
        <div className="guide-note"><h3>Survey and Restore are independent.</h3><p>An impact report does not create queue messages or change target health. The operator connects landscape context to the relevant recovery work.</p></div>
      </aside>
    </section>

    <section className="guide-disclosures" aria-label="Operator reference">
      <details className="panel"><summary><span>Understand recovery states</span><small>Ready, blocked, retrying and terminal work</small></summary><dl className="status-guide">{lifecycle.map(([state, description]) => <div key={state}><dt>{state}</dt><dd>{description}</dd></div>)}</dl></details>
      <details className="panel"><summary><span>How live updates and target circuits work</span><small>Connection, cooldown and evidence</small></summary><div className="reference-copy"><p><strong>Live updates are invalidations.</strong> SSE signals a committed change; the dashboard fetches fresh queue, statistics and circuit snapshots through REST. Reconnects trigger another snapshot. If the connection fails, the last snapshot stays visible with a warning.</p><p><strong>Open does not mean permanently failed.</strong> Three qualifying target failures open a circuit. After cooldown, one half-open probe may run. A successful probe closes the circuit; a failed probe opens it again. A closed circuit reflects persisted execution evidence, not continuous remote health monitoring.</p><p><strong>Latency has a defined scope.</strong> P95 is the nearest-rank receipt-to-success time for completed events in this database, including backoff. It is not a historical time-series chart or an external SLA measurement.</p></div></details>
      <details className="panel"><summary><span>Safety and system boundaries</span><small>What this local demonstration does and does not do</small></summary><div className="reference-copy"><p><strong>Keep automatic delivery disabled.</strong> Both supplied Compose configurations use <code>RESTORE_WORKER_ENABLED=false</code>. Manual recovery claims at most five messages per dashboard batch. Dependencies, leases, retry limits and circuit admission remain Java policies.</p><p><strong>Preserve the database.</strong> The operations loader is opt-in and refuses any nonempty database. It never replaces existing records. Repeat a fresh walkthrough with a new isolated Compose project; do not delete or prune the preserved volume.</p><p><strong>Delivery is at least once.</strong> Stable event IDs act as idempotency keys at the local simulator. Explicit prerequisites enforce partial order; independent messages have no FIFO completion guarantee.</p><p><strong>Fictional integrations only.</strong> This is a local portfolio system. There is no real SAP, CPI, Salesforce, customer data or credential access. Simulator settings are process-local and default to success after a restart; PostgreSQL evidence persists.</p></div></details>
    </section>
  </main>;
}

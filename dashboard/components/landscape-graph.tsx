"use client";

import { useId, useMemo, useRef, useState } from "react";

type SystemNode = { systemId: string; label: string; criticality: number; health: string };
type Integration = { integrationId: string; sourceSystemId: string; targetSystemId: string; protocol: string; dataDomain: string; slaClass: string };
type Lane = "business" | "core" | "integration" | "channel";
type PositionedNode = SystemNode & { x: number; y: number };
type Props = { systems: SystemNode[]; integrations: Integration[]; selectedSystemId: string; onSelect: (systemId: string) => void };

const lanes: Array<{ id: Lane; label: string; x: number }> = [
  { id: "business", label: "Business systems", x: 32 },
  { id: "core", label: "Core systems", x: 294 },
  { id: "integration", label: "CPI integration flows", x: 556 },
  { id: "channel", label: "Channels & services", x: 818 }
];
const categories: Record<Lane, string> = { business: "Business system", core: "Core system", integration: "CPI integration flow", channel: "Channel / service" };
function laneFor(systemId: string): Lane {
  if (systemId.startsWith("CPI_")) return "integration";
  if (systemId === "SUPPLIER_PORTAL" || systemId === "SALESFORCE") return "business";
  if (systemId.startsWith("SAP_") || systemId === "LEGACY_WMS") return "core";
  return "channel";
}
function route(source: PositionedNode, target: PositionedNode) {
  // Same-lane links travel through the gutter instead of cutting across cards.
  if (source.x === target.x) {
    const gutter = source.x + 226;
    return `M ${source.x + 204} ${source.y + 20} C ${gutter} ${source.y + 20}, ${gutter} ${target.y + 40}, ${target.x + 204} ${target.y + 40}`;
  }
  const rightward = target.x > source.x;
  const startX = source.x + (rightward ? 204 : 0);
  const endX = target.x + (rightward ? 0 : 204);
  // Separate the ports of opposite-direction links so cycles remain visible.
  const offset = rightward ? 20 : 40;
  const bend = Math.max(52, Math.abs(endX - startX) * 0.36);
  return `M ${startX} ${source.y + offset} C ${startX + (rightward ? bend : -bend)} ${source.y + offset}, ${endX + (rightward ? -bend : bend)} ${target.y + offset}, ${endX} ${target.y + offset}`;
}

export function LandscapeGraph({ systems, integrations, selectedSystemId, onSelect }: Props) {
  const [view, setView] = useState<"focus" | "landscape">("focus");
  const svg = useRef<SVGSVGElement>(null);
  const compact = useRef<HTMLDivElement>(null);
  const id = useId();
  const selected = systems.find(system => system.systemId === selectedSystemId);
  const incoming = integrations.filter(edge => edge.targetSystemId === selectedSystemId);
  const outgoing = integrations.filter(edge => edge.sourceSystemId === selectedSystemId);
  const relatedIds = new Set([selectedSystemId, ...incoming.map(edge => edge.sourceSystemId), ...outgoing.map(edge => edge.targetSystemId)]);
  const reciprocal = incoming.some(edge => outgoing.some(other => other.targetSystemId === edge.sourceSystemId));
  const nodes = useMemo(() => {
    const grouped = new Map<Lane, SystemNode[]>(lanes.map(lane => [lane.id, []]));
    systems.forEach(system => grouped.get(laneFor(system.systemId))?.push(system));
    return lanes.flatMap(lane => (grouped.get(lane.id) ?? []).sort((a, b) => b.criticality - a.criticality || a.label.localeCompare(b.label)).map((system, index, group) => ({ ...system, x: lane.x, y: 75 + index * (352 / Math.max(group.length - 1, 1)) })));
  }, [systems]);
  const positioned = new Map(nodes.map(node => [node.systemId, node]));
  const rowCount = Math.max(3, incoming.length, outgoing.length);
  const height = 92 + rowCount * 108;
  const centerY = 68 + (rowCount - 1) * 54;
  function select(systemId: string) {
    onSelect(systemId);
    // Direct view changes its node set; retain keyboard focus on the new origin.
    window.requestAnimationFrame(() => {
      const graph = view === "focus" && window.matchMedia("(max-width: 600px)").matches ? compact.current : svg.current;
      graph?.querySelector<HTMLElement | SVGGElement>('[data-system-id][aria-pressed="true"]')?.focus();
    });
  }
  function node(system: PositionedNode, direction?: "upstream" | "downstream" | "selected", key = system.systemId) {
    const focused = system.systemId === selectedSystemId;
    const full = direction === undefined;
    return <g key={key} data-system-id={system.systemId} data-direction={direction} className={`map-node ${focused ? "selected" : ""} ${!relatedIds.has(system.systemId) ? "inactive" : ""}`} transform={`translate(${system.x}, ${system.y})`} tabIndex={0} role="button" aria-label={`Select ${system.label}${direction && direction !== "selected" ? ` (${direction})` : ""}`} aria-pressed={focused} onClick={() => select(system.systemId)} onKeyDown={event => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); select(system.systemId); } }}>
      <rect width={full ? 204 : 236} height={full ? 60 : 76} rx="6" />
      <rect className="criticality-bar" width={Math.max(4, system.criticality * 1.8)} height="3" rx="1.5" />
      <text className="node-name" x="13" y="24">{system.label}</text>
      <text className="node-meta" x="13" y="43">{categories[laneFor(system.systemId)]}</text>
      {full ? <text className="criticality" x="183" y="43">C{system.criticality}</text> : <text className="node-meta" x="13" y="62">{system.health.replaceAll("_", " ").toLowerCase()} · Criticality {system.criticality}/10</text>}
      <title>{system.label} · {system.systemId} · criticality {system.criticality}/10 · {system.health.toLowerCase()}</title>
    </g>;
  }
  function compactNode(system: SystemNode, direction: "upstream" | "downstream" | "selected") {
    return <button className={"compact-node " + (direction === "selected" ? "selected" : "")} data-system-id={system.systemId} aria-pressed={direction === "selected"} aria-label={`Select ${system.label}${direction === "selected" ? "" : ` (${direction})`}`} onClick={() => select(system.systemId)}><strong>{system.label}</strong><span>{categories[laneFor(system.systemId)]}</span><small>{system.health.replaceAll("_", " ").toLowerCase()} · Criticality {system.criticality}/10</small></button>;
  }
  function compactPaths(edges: Integration[], direction: "upstream" | "downstream") {
    return <section aria-label={direction === "upstream" ? "Upstream paths" : "Downstream paths"}><h3>{direction === "upstream" ? "Upstream" : "Downstream"} <span>{edges.length} {edges.length === 1 ? "path" : "paths"}</span></h3>{edges.map(edge => {
      const other = positioned.get(direction === "upstream" ? edge.sourceSystemId : edge.targetSystemId);
      return other ? <div className="compact-path" key={edge.integrationId} data-integration-id={edge.integrationId}>{compactNode(other, direction)}<span className="compact-protocol">{edge.protocol}</span></div> : null;
    })}{edges.length === 0 && <p>No {direction} paths</p>}</section>;
  }
  const markers = <defs>{["upstream", "downstream"].map(direction => <marker key={direction} id={`${id}-${direction}`} className={direction + "-arrow"} viewBox="0 -5 10 10" refX="9" refY="0" markerWidth="6" markerHeight="6" orient="auto"><path d="M0,-4L8,0L0,4" /></marker>)}</defs>;
  return <>
    <div className="graph-tools">
      <div className="graph-view-switch" role="group" aria-label="Topology view"><button aria-pressed={view === "focus"} onClick={() => setView("focus")}>Direct connections</button><button aria-pressed={view === "landscape"} onClick={() => setView("landscape")}>Full landscape</button></div>
      <span>{incoming.length} upstream · {outgoing.length} downstream paths</span>
    </div>
    <div className={`graph-scroll ${view === "focus" ? "direct-scroll" : ""}`} tabIndex={0} role="region" aria-label="Scrollable landscape topology">
      {view === "focus" ? <svg ref={svg} className="graph focus-graph" viewBox={`0 0 1040 ${height}`} role="group" aria-label={`Direct connections for ${selected?.label ?? "selected system"}`}>
        {markers}
        {[{ x: 24, text: "Upstream", count: incoming.length }, { x: 402, text: "Selected system" }, { x: 780, text: "Downstream", count: outgoing.length }].map(column => <g key={column.x}><rect className="topology-lane" x={column.x - 8} y="16" width="252" height={height - 32} /><text className="lane-label" x={column.x + 4} y="43">{column.text}{column.count !== undefined ? ` / ${column.count} ${column.count === 1 ? "path" : "paths"}` : ""}</text></g>)}
        {([incoming, outgoing] as const).map((edges, side) => edges.map((edge, index) => {
          const upstream = side === 0;
          const other = positioned.get(upstream ? edge.sourceSystemId : edge.targetSystemId);
          if (!other) return null;
          const y = 68 + (index + (rowCount - edges.length) / 2) * 108;
          const portY = centerY + 20 + ((index + 1) / (edges.length + 1)) * 36;
          const path = upstream ? `M 260 ${y + 38} H 334 C 367 ${y + 38}, 369 ${portY}, 400 ${portY}` : `M 638 ${portY} C 671 ${portY}, 673 ${y + 38}, 706 ${y + 38} H 778`;
          const direction = upstream ? "upstream" : "downstream";
          return <g key={edge.integrationId}>
            <g className="links"><path data-integration-id={edge.integrationId} data-source={edge.sourceSystemId} data-target={edge.targetSystemId} className={`focused-link ${upstream ? "upstream-link" : ""}`} d={path} markerEnd={`url(#${id}-${direction})`}><title>{edge.protocol} · {edge.dataDomain} · {edge.slaClass}</title></path></g>
            <g className="edge-label" transform={`translate(${upstream ? 300 : 738}, ${y + 38})`}><rect x="-26" y="-10" width="52" height="20" rx="4" /><text textAnchor="middle" y="4">{edge.protocol}</text></g>
            {node({ ...other, x: upstream ? 24 : 780, y }, direction, edge.integrationId)}
          </g>;
        }))}
        {selected && node({ ...selected, x: 402, y: centerY }, "selected")}
        {incoming.length === 0 && <text className="empty-topology" x="142" y={centerY + 38} textAnchor="middle">No upstream paths</text>}
        {outgoing.length === 0 && <text className="empty-topology" x="898" y={centerY + 38} textAnchor="middle">No downstream paths</text>}
      </svg> : <svg ref={svg} className="graph landscape-graph" viewBox="0 0 1054 514" role="group" aria-label="Interactive directed integration topology">
        {markers}
        {lanes.map(lane => <g key={lane.id}><rect className="topology-lane" x={lane.x} y="25" width="204" height="470" /><text className="lane-label" x={lane.x + 14} y="52">{lane.label}</text></g>)}
        <g className="links">{[...integrations].sort((a, b) => Number(a.sourceSystemId === selectedSystemId || a.targetSystemId === selectedSystemId) - Number(b.sourceSystemId === selectedSystemId || b.targetSystemId === selectedSystemId)).map(edge => {
          const source = positioned.get(edge.sourceSystemId); const target = positioned.get(edge.targetSystemId);
          const focused = edge.sourceSystemId === selectedSystemId || edge.targetSystemId === selectedSystemId;
          const upstream = edge.targetSystemId === selectedSystemId;
          return source && target ? <path data-integration-id={edge.integrationId} className={focused ? upstream ? "focused-link upstream-link" : "focused-link" : ""} key={edge.integrationId} d={route(source, target)} markerEnd={`url(#${id}-${upstream ? "upstream" : "downstream"})`}><title>{edge.dataDomain} · {source.label} → {target.label}</title></path> : null;
        })}</g>
        <g>{nodes.map(system => node(system))}</g>
      </svg>}
    </div>
    {view === "focus" && <div className="compact-topology" ref={compact}>
      {compactPaths(incoming, "upstream")}
      <p className="compact-direction">↓ Data flows into the selection</p>
      <section aria-label="Selected system in topology"><h3>Selected system</h3>{selected && compactNode(selected, "selected")}</section>
      <p className="compact-direction">↓ Data flows out to downstream systems</p>
      {compactPaths(outgoing, "downstream")}
    </div>}
    <div className="graph-caption"><span>Focus: <strong>{selected?.label ?? "No system selected"}</strong></span><span>Dashed = upstream · Solid = downstream · Arrows follow data.</span></div>
    {view === "landscape" && <p className="graph-pan-hint">Scroll horizontally to explore all systems.</p>}
    <p className="graph-scope">{view === "focus" ? "Direct paths only. Analyze an outage to assess downstream impact." : "All registered systems and paths. Use Direct connections to trace the selection."}{view === "focus" && reciprocal ? " Systems connected in both directions appear on both sides." : ""}</p>
  </>;
}

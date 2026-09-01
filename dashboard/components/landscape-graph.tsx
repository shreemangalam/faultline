"use client";

import { useMemo, useState } from "react";

type SystemNode = { systemId: string; label: string; criticality: number; health: string };
type Integration = { integrationId: string; sourceSystemId: string; targetSystemId: string; protocol: string; dataDomain: string; slaClass: string };
type Lane = "business" | "core" | "integration" | "channel";
type PositionedNode = SystemNode & { x: number; y: number; lane: Lane };
type Props = { systems: SystemNode[]; integrations: Integration[]; selectedSystemId: string; onSelect: (systemId: string) => void };

const lanes: Array<{ id: Lane; label: string; x: number }> = [
  { id: "business", label: "Business inputs", x: 32 },
  { id: "core", label: "Core systems", x: 294 },
  { id: "integration", label: "CPI integration flows", x: 556 },
  { id: "channel", label: "Channels & services", x: 818 }
];

function laneFor(systemId: string): Lane {
  if (systemId.startsWith("CPI_")) return "integration";
  if (systemId === "SUPPLIER_PORTAL" || systemId === "SALESFORCE") return "business";
  if (systemId.startsWith("SAP_") || systemId === "LEGACY_WMS") return "core";
  return "channel";
}

function route(source: PositionedNode, target: PositionedNode) {
  const rightward = target.x > source.x;
  const startX = source.x + (rightward ? 204 : 0);
  const endX = target.x + (rightward ? 0 : 204);
  const bend = Math.max(52, Math.abs(endX - startX) * 0.36);
  return `M ${startX} ${source.y + 26} C ${startX + (rightward ? bend : -bend)} ${source.y + 26}, ${endX + (rightward ? -bend : bend)} ${target.y + 26}, ${endX} ${target.y + 26}`;
}

export function LandscapeGraph({ systems, integrations, selectedSystemId, onSelect }: Props) {
  const [hoveredSystemId, setHoveredSystemId] = useState<string | null>(null);
  const nodes = useMemo(() => {
    const grouped = new Map<Lane, SystemNode[]>(lanes.map((lane) => [lane.id, []]));
    systems.forEach((system) => grouped.get(laneFor(system.systemId))?.push(system));
    return lanes.flatMap((lane) => (grouped.get(lane.id) ?? []).sort((a, b) => b.criticality - a.criticality || a.label.localeCompare(b.label)).map((system, index, group) => ({ ...system, lane: lane.id, x: lane.x, y: 104 + index * (452 / Math.max(group.length - 1, 1)) })));
  }, [systems]);
  const positioned = useMemo(() => new Map(nodes.map((node) => [node.systemId, node])), [nodes]);
  const focusId = hoveredSystemId ?? selectedSystemId;
  const relatedSystemIds = useMemo(() => new Set(integrations.flatMap((integration) => integration.sourceSystemId === focusId || integration.targetSystemId === focusId ? [integration.sourceSystemId, integration.targetSystemId] : [])), [focusId, integrations]);

  return <svg className="graph" viewBox="0 0 1054 620" role="img" aria-label="Interactive directed integration topology">
    <defs><marker id="dependency-arrow" viewBox="0 -5 10 10" refX="9" refY="0" markerWidth="5" markerHeight="5" orient="auto"><path d="M0,-4L8,0L0,4" /></marker></defs>
    {lanes.map((lane) => <g key={lane.id}><rect className="topology-lane" x={lane.x} y="45" width="204" height="536" /><text className="lane-label" x={lane.x + 14} y="72">{lane.label}</text></g>)}
    <g className="links">{integrations.map((integration) => { const source = positioned.get(integration.sourceSystemId); const target = positioned.get(integration.targetSystemId); const focused = integration.sourceSystemId === focusId || integration.targetSystemId === focusId; return source && target ? <path className={focused ? "focused-link" : ""} key={integration.integrationId} d={route(source, target)} markerEnd="url(#dependency-arrow)"><title>{integration.dataDomain} · {source.label} → {target.label}</title></path> : null; })}</g>
    <g>{nodes.map((node) => { const focused = node.systemId === focusId; const related = relatedSystemIds.has(node.systemId); return <g className={`map-node ${node.systemId === selectedSystemId ? "selected" : ""} ${related ? "related" : ""} ${focusId && !related ? "inactive" : ""}`} key={node.systemId} transform={`translate(${node.x}, ${node.y})`} onClick={() => onSelect(node.systemId)} onMouseEnter={() => setHoveredSystemId(node.systemId)} onMouseLeave={() => setHoveredSystemId(null)} onFocus={() => setHoveredSystemId(node.systemId)} onBlur={() => setHoveredSystemId(null)} tabIndex={0} role="button" aria-label={`Select ${node.label}`} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") onSelect(node.systemId); }}><rect width="204" height="52" rx="4" /><rect className="criticality-bar" width={Math.max(4, node.criticality * 1.8)} height="3" rx="1.5" /><text className="node-name" x="13" y="23">{node.label}</text><text className="node-meta" x="13" y="40">{node.systemId.replace("CPI_", "CPI · ")}</text><text className="criticality" x="183" y="23">C{node.criticality}</text><title>{node.label} · criticality {node.criticality}</title></g>; })}</g>
  </svg>;
}
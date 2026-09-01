"use client";

import * as d3 from "d3";
import { useEffect, useMemo, useState } from "react";

type SystemNode = { systemId: string; label: string; criticality: number; health: string };
type Integration = { integrationId: string; sourceSystemId: string; targetSystemId: string; protocol: string; dataDomain: string; slaClass: string };
type PositionedNode = SystemNode & d3.SimulationNodeDatum;
type Props = { systems: SystemNode[]; integrations: Integration[]; selectedSystemId: string; onSelect: (systemId: string) => void };

function systemLabel(label: string) { return label.length > 16 ? label.replace(" Integration", "").replace(" Service", "") : label; }

export function LandscapeGraph({ systems, integrations, selectedSystemId, onSelect }: Props) {
  const [nodes, setNodes] = useState<PositionedNode[]>([]);
  const [hoveredSystemId, setHoveredSystemId] = useState<string | null>(null);

  useEffect(() => {
    const nextNodes: PositionedNode[] = systems.map((system) => ({ ...system }));
    const links = integrations.map((integration) => ({ source: integration.sourceSystemId, target: integration.targetSystemId }));
    const simulation = d3.forceSimulation<PositionedNode>(nextNodes)
      .force("link", d3.forceLink<PositionedNode, { source: string; target: string }>(links).id((node) => node.systemId).distance(116).strength(0.62))
      .force("charge", d3.forceManyBody<PositionedNode>().strength(-350))
      .force("center", d3.forceCenter(480, 280))
      .force("collision", d3.forceCollide<PositionedNode>((node) => 27 + node.criticality * 1.2))
      .force("radial", d3.forceRadial<PositionedNode>((node) => 130 + (10 - node.criticality) * 12, 480, 280).strength(0.045)).stop();
    for (let tick = 0; tick < 240; tick += 1) simulation.tick();
    setNodes(nextNodes);
    return () => { simulation.stop(); };
  }, [systems, integrations]);

  const positioned = useMemo(() => new Map(nodes.map((node) => [node.systemId, node])), [nodes]);
  const focusId = hoveredSystemId ?? selectedSystemId;
  const relatedSystemIds = useMemo(() => new Set(integrations.flatMap((integration) => integration.sourceSystemId === focusId ? [integration.sourceSystemId, integration.targetSystemId] : integration.targetSystemId === focusId ? [integration.sourceSystemId, integration.targetSystemId] : [])), [focusId, integrations]);

  return <svg className="graph" viewBox="0 0 960 560" role="img" aria-label="Interactive directed integration landscape graph">
    <defs><marker id="dependency-arrow" viewBox="0 -5 10 10" refX="18" refY="0" markerWidth="6" markerHeight="6" orient="auto"><path d="M0,-5L10,0L0,5" /></marker></defs>
    <g className="links">{integrations.map((integration) => { const source = positioned.get(integration.sourceSystemId); const target = positioned.get(integration.targetSystemId); const focused = integration.sourceSystemId === focusId || integration.targetSystemId === focusId; return source && target ? <line className={focused ? "focused-link" : ""} key={integration.integrationId} x1={source.x} y1={source.y} x2={target.x} y2={target.y} markerEnd="url(#dependency-arrow)"><title>{integration.integrationId}: {integration.dataDomain} via {integration.protocol}</title></line> : null; })}</g>
    <g>{nodes.map((node) => { const focused = node.systemId === focusId; const related = relatedSystemIds.has(node.systemId); return <g className={`node ${node.systemId === selectedSystemId ? "selected" : ""} ${related ? "related" : ""} ${focusId && !related ? "muted-node" : ""}`} key={node.systemId} transform={`translate(${node.x ?? 0},${node.y ?? 0})`} onClick={() => onSelect(node.systemId)} onMouseEnter={() => setHoveredSystemId(node.systemId)} onMouseLeave={() => setHoveredSystemId(null)} tabIndex={0} role="button" aria-label={`Select ${node.label}`} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") onSelect(node.systemId); }} onFocus={() => setHoveredSystemId(node.systemId)} onBlur={() => setHoveredSystemId(null)}><circle r={15 + node.criticality * 1.15} /><circle className="node-core" r="3" /><text className="node-id" y="4">{node.systemId.replace("CPI_", "")}</text>{focused && <text className="node-label" y={31 + node.criticality * 1.15}>{systemLabel(node.label)}</text>}<title>{node.label} · criticality {node.criticality}</title></g>; })}</g>
  </svg>;
}
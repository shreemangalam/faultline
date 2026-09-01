"use client";
import * as d3 from "d3";
import { useEffect, useMemo, useState } from "react";

type SystemNode = { systemId: string; label: string; criticality: number; health: string };
type Integration = { integrationId: string; sourceSystemId: string; targetSystemId: string; protocol: string; dataDomain: string; slaClass: string };
type PositionedNode = SystemNode & d3.SimulationNodeDatum;
type Props = { systems: SystemNode[]; integrations: Integration[]; selectedSystemId: string; onSelect: (systemId: string) => void };

export function LandscapeGraph({ systems, integrations, selectedSystemId, onSelect }: Props) {
  const [nodes, setNodes] = useState<PositionedNode[]>([]);
  useEffect(() => {
    const nextNodes: PositionedNode[] = systems.map((system) => ({ ...system }));
    const byId = new Map(nextNodes.map((node) => [node.systemId, node]));
    const links: d3.SimulationLinkDatum<PositionedNode>[] = integrations.map((integration) => ({ source: byId.get(integration.sourceSystemId)!, target: byId.get(integration.targetSystemId)! }));
    const simulation = d3.forceSimulation<PositionedNode>(nextNodes)
      .force("link", d3.forceLink<PositionedNode, d3.SimulationLinkDatum<PositionedNode>>(links).distance(82).strength(0.65))
      .force("charge", d3.forceManyBody<PositionedNode>().strength(-210))
      .force("center", d3.forceCenter(390, 230))
      .force("collision", d3.forceCollide<PositionedNode>(29)).stop();
    for (let tick = 0; tick < 170; tick += 1) simulation.tick();
    setNodes(nextNodes);
    return () => { simulation.stop(); };
  }, [systems, integrations]);
  const positioned = useMemo(() => new Map(nodes.map((node) => [node.systemId, node])), [nodes]);
  return <svg className="graph" viewBox="0 0 780 460" role="img" aria-label="Interactive integration landscape graph">
    <g className="links">{integrations.map((integration) => { const source = positioned.get(integration.sourceSystemId); const target = positioned.get(integration.targetSystemId); return source && target ? <line key={integration.integrationId} x1={source.x} y1={source.y} x2={target.x} y2={target.y}><title>{integration.integrationId}: {integration.dataDomain} via {integration.protocol}</title></line> : null; })}</g>
    <g>{nodes.map((node) => <g className={`node ${node.systemId === selectedSystemId ? "selected" : ""}`} key={node.systemId} transform={`translate(${node.x ?? 0},${node.y ?? 0})`} onClick={() => onSelect(node.systemId)} tabIndex={0} role="button" aria-label={`Select ${node.label}`} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") onSelect(node.systemId); }}><circle r={16 + node.criticality * 0.75} /><text y="4">{node.systemId.replace("CPI_", "")}</text><title>{node.label} · criticality {node.criticality}</title></g>)}</g>
  </svg>;
}
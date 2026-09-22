import { useState } from 'react'
import type { NicTreeNodeResponse } from '../types'

interface Props {
  nodes: NicTreeNodeResponse[]
}

/**
 * Collapsible NIC tree per UI/UX Addendum §2.2. Cheap: the backend caches the
 * full tree in memory (Kickoff C1 AC 10) and this component just renders it.
 */
export function NicTreeView({ nodes }: Props) {
  return (
    <ul className="text-sm">
      {nodes.map((n) => (
        <TreeItem key={n.id} node={n} depth={0} />
      ))}
    </ul>
  )
}

function TreeItem({ node, depth }: { node: NicTreeNodeResponse; depth: number }) {
  const [open, setOpen] = useState(depth < 1)
  const hasChildren = node.children && node.children.length > 0
  return (
    <li className="py-0.5">
      <div
        className="flex items-center gap-2"
        style={{ paddingLeft: depth * 16 }}
      >
        {hasChildren ? (
          <button
            type="button"
            className="w-4 text-xs text-muted-foreground"
            onClick={() => setOpen((o) => !o)}
          >
            {open ? '▾' : '▸'}
          </button>
        ) : (
          <span className="inline-block w-4" />
        )}
        <span className="font-mono text-xs w-14 text-muted-foreground">{node.code}</span>
        <span>{node.description}</span>
        {node.is_primary ? <span className="text-amber-500">★</span> : null}
        {!node.active ? <span className="text-xs text-muted-foreground">(inactive)</span> : null}
      </div>
      {hasChildren && open ? (
        <ul>
          {node.children.map((c) => (
            <TreeItem key={c.id} node={c} depth={depth + 1} />
          ))}
        </ul>
      ) : null}
    </li>
  )
}

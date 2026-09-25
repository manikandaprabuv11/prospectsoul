import { ChevronDown, ChevronRight, Star } from 'lucide-react'
import { useState } from 'react'
import type { NicTreeNodeResponse } from '../types'

interface Props {
  nodes: NicTreeNodeResponse[]
}

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
    <li>
      <div
        className="flex items-center gap-2 rounded-lg px-2 py-1 hover:bg-surface-1 transition-colors"
        style={{ paddingLeft: depth * 20 + 8 }}
      >
        {hasChildren ? (
          <button
            type="button"
            className="flex size-5 items-center justify-center rounded text-muted-foreground hover:text-foreground hover:bg-surface-2 transition-colors"
            onClick={() => setOpen((o) => !o)}
          >
            {open ? <ChevronDown className="size-3.5" /> : <ChevronRight className="size-3.5" />}
          </button>
        ) : (
          <span className="inline-block size-5" />
        )}
        <span className="font-mono text-xs text-muted-foreground w-14 tabular-nums">{node.code}</span>
        <span className="flex-1">{node.description}</span>
        {node.is_primary ? <Star className="size-3.5 text-accent-amber fill-accent-amber" /> : null}
        {!node.active ? (
          <span className="rounded-md bg-surface-1 border border-border px-1.5 py-0.5 text-[10px] font-semibold text-muted-foreground">
            INACTIVE
          </span>
        ) : null}
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

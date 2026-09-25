import { Button } from '@/components/ui/button'
import { FileUp, Loader2 } from 'lucide-react'
import { useRef, useState } from 'react'
import { useImportNicFile } from '../hooks'
import type { NicImportResultResponse } from '../types'

export function NicImportPanel() {
  const inputRef = useRef<HTMLInputElement>(null)
  const [result, setResult] = useState<NicImportResultResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const mutation = useImportNicFile()

  async function handle(file: File | null) {
    if (!file) return
    setError(null)
    setResult(null)
    try {
      const res = await mutation.mutateAsync(file)
      setResult(res)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Import failed')
    } finally {
      if (inputRef.current) inputRef.current.value = ''
    }
  }

  return (
    <div className="rounded-xl border border-border bg-card p-4 shadow-card space-y-3">
      <div className="flex items-center justify-between gap-4">
        <div className="min-w-0">
          <h3 className="text-sm font-semibold">Import NIC master (.xlsx / .csv)</h3>
          <p className="text-xs text-muted-foreground leading-relaxed mt-0.5">
            Re-import is idempotent. Existing codes are updated; new codes are added; parents are resolved by longest existing prefix.
          </p>
        </div>
        <Button size="sm" onClick={() => inputRef.current?.click()} disabled={mutation.isPending} className="shrink-0">
          {mutation.isPending ? <><Loader2 className="size-3.5 animate-spin" /> Importing…</> : <><FileUp className="size-3.5" /> Choose file</>}
        </Button>
        <input
          ref={inputRef}
          type="file"
          accept=".xlsx,.xls,.csv"
          className="hidden"
          onChange={(e) => handle(e.target.files?.[0] ?? null)}
        />
      </div>
      {error ? (
        <div className="rounded-lg border border-destructive/30 bg-destructive/5 px-3 py-2 text-sm font-medium text-destructive">{error}</div>
      ) : null}
      {result ? (
        <div className="grid grid-cols-5 gap-3 rounded-xl border border-border bg-surface-1/50 p-3">
          <StatItem label="Rows read" value={result.rows_read} />
          <StatItem label="Created" value={result.created} accent="text-accent-emerald" />
          <StatItem label="Updated" value={result.updated} accent="text-accent-sky" />
          <StatItem label="Unresolved" value={result.unresolved_parents} accent="text-accent-amber" />
          <StatItem label="Rejected" value={result.rejected} accent="text-accent-rose" />
        </div>
      ) : null}
    </div>
  )
}

function StatItem({ label, value, accent }: { label: string; value: number; accent?: string }) {
  return (
    <div>
      <div className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">{label}</div>
      <div className={`text-lg font-bold tabular-nums ${accent ?? 'text-foreground'}`}>{value}</div>
    </div>
  )
}

import { Button } from '@/components/ui/button'
import { useRef, useState } from 'react'
import { useImportNicFile } from '../hooks'
import type { NicImportResultResponse } from '../types'

/**
 * Import panel for the NIC master. Reference-data import — deliberately does
 * NOT create import_batches / import_rows rows (Kickoff constraint 6).
 */
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
    <div className="rounded-md border p-4 space-y-3">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-sm font-medium">Import NIC master (.xlsx / .csv)</h3>
          <p className="text-xs text-muted-foreground">
            Re-import is idempotent. Existing codes are updated; new codes are added; parents are resolved by longest existing prefix.
          </p>
        </div>
        <Button size="sm" onClick={() => inputRef.current?.click()} disabled={mutation.isPending}>
          {mutation.isPending ? 'Importing…' : 'Choose file'}
        </Button>
        <input
          ref={inputRef}
          type="file"
          accept=".xlsx,.xls,.csv"
          className="hidden"
          onChange={(e) => handle(e.target.files?.[0] ?? null)}
        />
      </div>
      {error ? <p className="text-sm text-destructive">{error}</p> : null}
      {result ? (
        <div className="text-xs text-muted-foreground grid grid-cols-5 gap-3">
          <div>Rows read: <span className="font-semibold text-foreground">{result.rows_read}</span></div>
          <div>Created: <span className="font-semibold text-foreground">{result.created}</span></div>
          <div>Updated: <span className="font-semibold text-foreground">{result.updated}</span></div>
          <div>Unresolved parents: <span className="font-semibold text-foreground">{result.unresolved_parents}</span></div>
          <div>Rejected: <span className="font-semibold text-foreground">{result.rejected}</span></div>
        </div>
      ) : null}
    </div>
  )
}

import { Button } from '@/components/ui/button'
import { Pagination } from '@/shared/components/Pagination'
import { Plus, FileUp, LayoutList, Loader2, Search, TreePine } from 'lucide-react'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { useSearchParams } from 'react-router'
import { useMemo, useRef, useState } from 'react'
import { NicCodeDialog } from '../components/NicCodeDialog'
import { NicTreeView } from '../components/NicTreeView'
import {
  useCreateNicCode,
  useImportNicFile,
  useNicCodeList,
  useNicTree,
  useToggleNicPrimary,
  useUpdateNicCode,
} from '../hooks'
import type { IndustryType, NicCodeResponse, NicImportResultResponse, NicListFilters } from '../types'

export function NicCodesPage() {
  const [sp, setSp] = useSearchParams()
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editing, setEditing] = useState<NicCodeResponse | null>(null)
  const [dialogError, setDialogError] = useState<string | undefined>()
  const importRef = useRef<HTMLInputElement>(null)
  const importMutation = useImportNicFile()
  const [importResult, setImportResult] = useState<NicImportResultResponse | null>(null)
  const [importError, setImportError] = useState<string | null>(null)

  const view = sp.get('view') ?? 'table'
  const filters: NicListFilters = useMemo(
    () => ({
      q: sp.get('q') ?? undefined,
      level: sp.get('level') ? Number(sp.get('level')) : undefined,
      industry_type: (sp.get('industry_type') as IndustryType | null) ?? undefined,
      is_primary: sp.get('is_primary') === 'true' ? true : undefined,
      active: sp.get('active') === 'false' ? false : sp.get('active') === 'true' ? true : undefined,
      page: Number(sp.get('page') ?? '0'),
      size: 50,
    }),
    [sp],
  )

  const list = useNicCodeList(filters)
  const tree = useNicTree(undefined, 3)
  const create = useCreateNicCode()
  const togglePrimary = useToggleNicPrimary()
  const update = useUpdateNicCode(editing?.id ?? '')

  function updateParam(key: string, value: string | undefined) {
    const next = new URLSearchParams(sp)
    if (value === undefined || value === '') next.delete(key)
    else next.set(key, value)
    if (key !== 'page') next.set('page', '0')
    setSp(next)
  }

  function setPage(p: number) {
    updateParam('page', String(p))
  }

  async function handleImport(file: File | null) {
    if (!file) return
    setImportError(null)
    setImportResult(null)
    try {
      const res = await importMutation.mutateAsync(file)
      setImportResult(res)
    } catch (err) {
      setImportError(err instanceof Error ? err.message : 'Import failed')
    } finally {
      if (importRef.current) importRef.current.value = ''
    }
  }

  async function handleSave(payload: {
    code: string
    description: string
    industry_type: IndustryType
    is_primary: boolean
    active: boolean
  }) {
    setDialogError(undefined)
    try {
      if (editing) {
        await update.mutateAsync({
          description: payload.description,
          industry_type: payload.industry_type,
          is_primary: payload.is_primary,
          active: payload.active,
        })
      } else {
        await create.mutateAsync({
          code: payload.code,
          description: payload.description,
          industry_type: payload.industry_type,
          is_primary: payload.is_primary,
        })
      }
      setDialogOpen(false)
      setEditing(null)
    } catch (err) {
      setDialogError(err instanceof Error ? err.message : 'Save failed')
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between gap-4">
        <div className="min-w-0">
          <h1 className="text-xl font-bold tracking-tight text-foreground">NIC Code Master</h1>
          <p className="text-xs text-muted-foreground mt-0.5">Reference data — primary codes surface first in filter pickers.</p>
        </div>
        <div className="flex items-center gap-2 shrink-0">
          <div className="inline-flex rounded-lg border border-border bg-surface-1 p-0.5">
            <Button
              variant={view === 'table' ? 'default' : 'ghost'}
              size="sm"
              onClick={() => updateParam('view', 'table')}
            >
              <LayoutList className="size-3.5" /> Table
            </Button>
            <Button
              variant={view === 'tree' ? 'default' : 'ghost'}
              size="sm"
              onClick={() => updateParam('view', 'tree')}
            >
              <TreePine className="size-3.5" /> Tree
            </Button>
          </div>
          <div className="h-6 w-px bg-border" />
          <Button variant="outline" size="sm" onClick={() => importRef.current?.click()} disabled={importMutation.isPending}>
            {importMutation.isPending ? <><Loader2 className="size-3.5 animate-spin" /> Importing…</> : <><FileUp className="size-3.5" /> Import master</>}
          </Button>
          <input
            ref={importRef}
            type="file"
            accept=".xlsx,.xls,.csv"
            className="hidden"
            onChange={(e) => handleImport(e.target.files?.[0] ?? null)}
          />
          <Button size="sm" onClick={() => { setEditing(null); setDialogError(undefined); setDialogOpen(true) }}>
            <Plus className="size-3.5" /> Add code
          </Button>
        </div>
      </div>

      {importError && (
        <div className="rounded-lg border border-destructive/30 bg-destructive/5 px-3 py-2 text-sm font-medium text-destructive">{importError}</div>
      )}
      {importResult && (
        <div className="flex items-center gap-6 rounded-xl border border-border bg-surface-1/50 px-4 py-2.5 text-sm">
          <ImportStat label="Read" value={importResult.rows_read} />
          <ImportStat label="Created" value={importResult.created} accent="text-accent-emerald" />
          <ImportStat label="Updated" value={importResult.updated} accent="text-accent-sky" />
          <ImportStat label="Unresolved" value={importResult.unresolved_parents} accent="text-accent-amber" />
          <ImportStat label="Rejected" value={importResult.rejected} accent="text-accent-rose" />
          <button type="button" onClick={() => setImportResult(null)} className="ml-auto text-xs text-muted-foreground hover:text-foreground transition-colors">Dismiss</button>
        </div>
      )}

      {view === 'table' ? (
        <div className="space-y-4">
          <div className="rounded-xl border border-border bg-card p-4 shadow-card">
            <div className="grid gap-3 grid-cols-[minmax(0,1fr)_repeat(2,minmax(0,160px))_auto_auto]">
              <div className="min-w-0">
                <label htmlFor="nic-search" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Search</label>
                <div className="relative mt-1">
                  <Search className="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground/50" />
                  <Input
                    id="nic-search"
                    placeholder="Code or description…"
                    className="pl-8"
                    value={filters.q ?? ''}
                    onChange={(e) => updateParam('q', e.target.value || undefined)}
                  />
                </div>
              </div>
              <div className="min-w-0">
                <label htmlFor="nic-level" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Level</label>
                <Select
                  id="nic-level"
                  className="mt-1"
                  value={filters.level?.toString() ?? ''}
                  onChange={(e) => updateParam('level', e.target.value || undefined)}
                >
                  <option value="">All levels</option>
                  <option value="1">1 — Section</option>
                  <option value="2">2 — Division</option>
                  <option value="3">3 — Group</option>
                  <option value="4">4 — Class</option>
                  <option value="5">5 — Sub-class</option>
                </Select>
              </div>
              <div className="min-w-0">
                <label htmlFor="nic-type" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Type</label>
                <Select
                  id="nic-type"
                  className="mt-1"
                  value={filters.industry_type ?? ''}
                  onChange={(e) => updateParam('industry_type', e.target.value || undefined)}
                >
                  <option value="">All types</option>
                  <option value="Manufacturing">Manufacturing</option>
                  <option value="Service">Service</option>
                  <option value="Unknown">Unknown</option>
                </Select>
              </div>
              <div className="flex items-end pb-0.5">
                <button
                  type="button"
                  onClick={() => updateParam('is_primary', filters.is_primary ? undefined : 'true')}
                  className={
                    'inline-flex items-center gap-1.5 rounded-lg px-3 h-9 text-xs font-medium transition-colors border ' +
                    (filters.is_primary
                      ? 'bg-accent-amber/10 text-accent-amber border-accent-amber/30'
                      : 'bg-transparent text-muted-foreground border-border hover:bg-accent hover:text-foreground')
                  }
                >
                  <span className="text-sm">{filters.is_primary ? '★' : '☆'}</span>
                  Primary
                </button>
              </div>
              <div className="flex items-end pb-0.5">
                <button
                  type="button"
                  onClick={() => updateParam('active', filters.active === false ? undefined : 'false')}
                  className={
                    'inline-flex items-center gap-1.5 rounded-lg px-3 h-9 text-xs font-medium transition-colors border ' +
                    (filters.active !== false
                      ? 'bg-accent-emerald/10 text-accent-emerald border-accent-emerald/30'
                      : 'bg-transparent text-muted-foreground border-border hover:bg-accent hover:text-foreground')
                  }
                >
                  <span className={`inline-flex size-2 rounded-full ${filters.active !== false ? 'bg-accent-emerald' : 'bg-muted-foreground/40'}`} />
                  Active
                </button>
              </div>
            </div>
          </div>

          {list.isLoading ? (
            <p className="text-sm text-muted-foreground py-4">Loading...</p>
          ) : list.isError ? (
            <p className="text-sm text-destructive py-4">Failed to load: {String(list.error)}</p>
          ) : list.data && list.data.content.length === 0 ? (
            <div className="rounded-xl border border-border bg-surface-1/50 py-12 text-center">
              <p className="font-semibold">No NIC codes yet</p>
              <p className="text-sm text-muted-foreground mt-1">Import the master file above or add one manually.</p>
            </div>
          ) : (
            <div className="overflow-hidden rounded-xl border border-border bg-card shadow-card">
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-border bg-surface-1">
                      <th className="py-3 px-3 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Code</th>
                      <th className="py-3 px-3 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Description</th>
                      <th className="py-3 px-3 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Level</th>
                      <th className="py-3 px-3 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Type</th>
                      <th className="py-3 px-3 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Primary</th>
                      <th className="py-3 px-3 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Active</th>
                      <th className="py-3 px-3 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground"></th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border">
                    {list.data?.content.map((row) => (
                      <tr key={row.id} className="hover:bg-accent/30 transition-colors duration-150 group/row">
                        <td className="py-2.5 px-3 font-mono text-xs font-semibold">{row.code}</td>
                        <td className="py-2.5 px-3">{row.description}</td>
                        <td className="py-2.5 px-3">
                          <span className="inline-flex items-center rounded-md bg-surface-1 px-2 py-0.5 text-[11px] font-semibold border border-border">
                            L{row.level}
                          </span>
                        </td>
                        <td className="py-2.5 px-3">
                          <span className={`inline-flex items-center rounded-md px-2 py-0.5 text-[11px] font-semibold ${
                            row.industry_type === 'Manufacturing' ? 'bg-accent-teal/10 text-accent-teal border border-accent-teal/20' :
                            row.industry_type === 'Service' ? 'bg-accent-violet/10 text-accent-violet border border-accent-violet/20' :
                            'bg-surface-1 text-muted-foreground border border-border'
                          }`}>
                            {row.industry_type}
                          </span>
                        </td>
                        <td className="py-2.5 px-3">
                          <button
                            type="button"
                            onClick={() => togglePrimary.mutate(row.id)}
                            className={`text-lg transition-colors duration-200 ${row.is_primary ? 'text-accent-amber hover:text-accent-amber/80' : 'text-muted-foreground/40 hover:text-accent-amber/60'}`}
                            aria-label="toggle primary"
                          >
                            {row.is_primary ? '★' : '☆'}
                          </button>
                        </td>
                        <td className="py-2.5 px-3">
                          <span className={`inline-flex size-2.5 rounded-full ${row.active ? 'bg-accent-emerald' : 'bg-muted-foreground/30'}`} />
                        </td>
                        <td className="py-2.5 px-3">
                          <Button
                            variant="ghost"
                            size="sm"
                            className="opacity-60 group-hover/row:opacity-100 transition-opacity"
                            onClick={() => {
                              setEditing(row)
                              setDialogError(undefined)
                              setDialogOpen(true)
                            }}
                          >
                            Edit
                          </Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {list.data ? (
            <Pagination
              page={list.data.page}
              totalPages={list.data.total_pages}
              totalElements={list.data.total_elements}
              itemLabel="codes"
              onPageChange={setPage}
            />
          ) : null}
        </div>
      ) : (
        <div className="rounded-xl border border-border bg-card p-4 shadow-card">
          {tree.isLoading ? (
            <p className="text-sm text-muted-foreground py-4">Loading tree...</p>
          ) : tree.isError ? (
            <p className="text-sm text-destructive py-4">Failed to load tree</p>
          ) : tree.data && tree.data.length === 0 ? (
            <div className="py-12 text-center">
              <p className="font-semibold">Tree is empty</p>
              <p className="text-sm text-muted-foreground mt-1">Import the master file to populate it.</p>
            </div>
          ) : (
            <NicTreeView nodes={tree.data ?? []} />
          )}
        </div>
      )}

      <NicCodeDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        onSave={handleSave}
        initial={editing}
        saving={create.isPending || update.isPending}
        errorMessage={dialogError}
      />
    </div>
  )
}

function ImportStat({ label, value, accent }: { label: string; value: number; accent?: string }) {
  return (
    <div className="flex items-center gap-1.5">
      <span className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">{label}</span>
      <span className={`text-sm font-bold tabular-nums ${accent ?? 'text-foreground'}`}>{value}</span>
    </div>
  )
}

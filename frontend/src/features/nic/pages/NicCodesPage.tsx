import { Button } from '@/components/ui/button'
import { PageHeader } from '@/components/layout/PageHeader'
import { Plus, LayoutList, Search, TreePine } from 'lucide-react'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { useSearchParams } from 'react-router'
import { useMemo, useState } from 'react'
import { NicCodeDialog } from '../components/NicCodeDialog'
import { NicImportPanel } from '../components/NicImportPanel'
import { NicTreeView } from '../components/NicTreeView'
import {
  useCreateNicCode,
  useNicCodeList,
  useNicTree,
  useToggleNicPrimary,
  useUpdateNicCode,
} from '../hooks'
import type { IndustryType, NicCodeResponse, NicListFilters } from '../types'

export function NicCodesPage() {
  const [sp, setSp] = useSearchParams()
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editing, setEditing] = useState<NicCodeResponse | null>(null)
  const [dialogError, setDialogError] = useState<string | undefined>()

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
    next.set('page', '0')
    setSp(next)
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
    <div className="space-y-6">
      <PageHeader
        title="NIC Code Master"
        description="Reference data — admin-editable. Primary codes surface first in filter pickers."
        actions={
          <>
            <div className="inline-flex rounded-lg border border-border bg-surface-1 p-0.5">
              <Button
                variant={view === 'table' ? 'default' : 'ghost'}
                size="sm"
                onClick={() => updateParam('view', 'table')}
              >
                <LayoutList /> Table
              </Button>
              <Button
                variant={view === 'tree' ? 'default' : 'ghost'}
                size="sm"
                onClick={() => updateParam('view', 'tree')}
              >
                <TreePine /> Tree
              </Button>
            </div>
            <Button
              onClick={() => { setEditing(null); setDialogError(undefined); setDialogOpen(true); }}
            >
              <Plus /> Add code
            </Button>
          </>
        }
      />

      <NicImportPanel />

      {view === 'table' ? (
        <div className="space-y-4">
          <div className="flex flex-wrap items-end gap-3 rounded-xl border border-border bg-card p-4 shadow-card">
            <div className="relative min-w-[240px]">
              <Search className="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground/50" />
              <Input
                placeholder="Search description or code prefix"
                className="pl-8"
                value={filters.q ?? ''}
                onChange={(e) => updateParam('q', e.target.value || undefined)}
              />
            </div>
            <Select
              value={filters.level?.toString() ?? ''}
              onChange={(e) => updateParam('level', e.target.value || undefined)}
            >
              <option value="">Any level</option>
              <option value="1">1 - Section</option>
              <option value="2">2 - Division</option>
              <option value="3">3 - Group</option>
              <option value="4">4 - Class</option>
              <option value="5">5 - Sub-class</option>
            </Select>
            <Select
              value={filters.industry_type ?? ''}
              onChange={(e) => updateParam('industry_type', e.target.value || undefined)}
            >
              <option value="">Any type</option>
              <option value="Manufacturing">Manufacturing</option>
              <option value="Service">Service</option>
              <option value="Unknown">Unknown</option>
            </Select>
            <label className="flex items-center gap-2 text-sm font-medium cursor-pointer">
              <input
                type="checkbox"
                checked={filters.is_primary === true}
                onChange={(e) => updateParam('is_primary', e.target.checked ? 'true' : undefined)}
                className="rounded"
              />
              Primary only
            </label>
            <label className="flex items-center gap-2 text-sm font-medium cursor-pointer">
              <input
                type="checkbox"
                checked={filters.active !== false}
                onChange={(e) => updateParam('active', e.target.checked ? undefined : 'false')}
                className="rounded"
              />
              Active only
            </label>
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
            <div className="flex items-center justify-between rounded-xl border border-border bg-card px-4 py-3 shadow-card text-sm">
              <span className="text-muted-foreground">
                Page <span className="font-semibold text-foreground tabular-nums">{list.data.page + 1}</span> of <span className="font-semibold text-foreground tabular-nums">{list.data.total_pages || 1}</span>
                {' · '}
                <span className="font-semibold text-foreground tabular-nums">{list.data.total_elements}</span> codes
              </span>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={list.data.page === 0}
                  onClick={() => updateParam('page', String(list.data.page - 1))}
                >
                  Prev
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={list.data.page + 1 >= list.data.total_pages}
                  onClick={() => updateParam('page', String(list.data.page + 1))}
                >
                  Next
                </Button>
              </div>
            </div>
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

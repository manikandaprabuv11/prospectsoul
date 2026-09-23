import { Button } from '@/components/ui/button'
import { PageHeader } from '@/components/layout/PageHeader'
import { Plus, LayoutList, TreePine } from 'lucide-react'
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

/**
 * `/settings/nic-codes` — UI/UX Addendum §2. Two views (Table + Tree)
 * toggled at the top. Admin-only; wired via RequireRole in the router.
 */
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
            <div className="inline-flex rounded-md border border-border/70 bg-background p-0.5 shadow-xs">
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
        <div className="space-y-3">
          <div className="flex flex-wrap items-end gap-3">
            <Input
              placeholder="Search description or code prefix"
              className="w-64"
              value={filters.q ?? ''}
              onChange={(e) => updateParam('q', e.target.value || undefined)}
            />
            <Select
              value={filters.level?.toString() ?? ''}
              onChange={(e) => updateParam('level', e.target.value || undefined)}
            >
              <option value="">Any level</option>
              <option value="1">1 · Section</option>
              <option value="2">2 · Division</option>
              <option value="3">3 · Group</option>
              <option value="4">4 · Class</option>
              <option value="5">5 · Sub-class</option>
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
            <label className="flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                checked={filters.is_primary === true}
                onChange={(e) => updateParam('is_primary', e.target.checked ? 'true' : undefined)}
              />
              Primary only
            </label>
            <label className="flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                checked={filters.active !== false}
                onChange={(e) => updateParam('active', e.target.checked ? undefined : 'false')}
              />
              Active only
            </label>
          </div>

          {list.isLoading ? (
            <p className="text-sm text-muted-foreground">Loading…</p>
          ) : list.isError ? (
            <p className="text-sm text-destructive">Failed to load: {String(list.error)}</p>
          ) : list.data && list.data.content.length === 0 ? (
            <p className="text-sm text-muted-foreground">
              No NIC codes yet — import the master file above or add one manually.
            </p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-muted-foreground border-b">
                    <th className="py-2 px-2">Code</th>
                    <th className="py-2 px-2">Description</th>
                    <th className="py-2 px-2">Level</th>
                    <th className="py-2 px-2">Type</th>
                    <th className="py-2 px-2">Primary</th>
                    <th className="py-2 px-2">Active</th>
                    <th className="py-2 px-2"></th>
                  </tr>
                </thead>
                <tbody>
                  {list.data?.content.map((row) => (
                    <tr key={row.id} className="border-b hover:bg-accent/30">
                      <td className="py-1.5 px-2 font-mono text-xs">{row.code}</td>
                      <td className="py-1.5 px-2">{row.description}</td>
                      <td className="py-1.5 px-2">{row.level}</td>
                      <td className="py-1.5 px-2">{row.industry_type}</td>
                      <td className="py-1.5 px-2">
                        <button
                          type="button"
                          onClick={() => togglePrimary.mutate(row.id)}
                          className={row.is_primary ? 'text-amber-500' : 'text-muted-foreground'}
                          aria-label="toggle primary"
                        >
                          {row.is_primary ? '★' : '☆'}
                        </button>
                      </td>
                      <td className="py-1.5 px-2">{row.active ? '●' : '○'}</td>
                      <td className="py-1.5 px-2">
                        <Button
                          variant="outline"
                          size="sm"
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
          )}

          {list.data ? (
            <div className="flex items-center gap-3 text-xs text-muted-foreground">
              <span>
                Page {list.data.page + 1} of {list.data.total_pages || 1} ·{' '}
                {list.data.total_elements} codes
              </span>
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
          ) : null}
        </div>
      ) : (
        <div>
          {tree.isLoading ? (
            <p className="text-sm text-muted-foreground">Loading tree…</p>
          ) : tree.isError ? (
            <p className="text-sm text-destructive">Failed to load tree</p>
          ) : tree.data && tree.data.length === 0 ? (
            <p className="text-sm text-muted-foreground">
              Tree is empty — import the master file to populate it.
            </p>
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

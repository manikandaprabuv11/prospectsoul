import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import { PageHeader } from '@/components/layout/PageHeader'
import { LoadingRows } from '@/components/feedback/LoadingRows'
import { ErrorState } from '@/components/feedback/ErrorState'
import { EmptyState } from '@/components/feedback/EmptyState'
import { InlineTip } from '@/components/feedback/InlineTip'
import { Filter, Plus, Trash2 } from 'lucide-react'
import { useState } from 'react'
import {
  useCompanyDefaults,
  useCreateCompanyDefault,
  useDeleteCompanyDefault,
  useUpdateCompanyDefault,
} from '../hooks'
import type { CompanyDefaultFilter, FilterOperator } from '../types'
import { FILTER_KEY_OPTIONS } from '../types'

/**
 * `/settings/company-defaults` — admin-managed default filters that
 * automatically apply to the Companies list on load. Users can override
 * any default from the filter panel; only ACTIVE rows here are applied.
 */
export function CompanyDefaultsPage() {
  const { data, isLoading, isError, error } = useCompanyDefaults()

  return (
    <div className="space-y-6">
      <PageHeader
        title="Companies · default filters"
        description="Applied automatically the moment the Companies list loads. Toggle Active off to disable one without deleting it."
        actions={<AddButton />}
      />

      <InlineTip>
        Values are stored as JSON — a scalar like <code>10</code>, a string like <code>"READY"</code>,
        or a boolean like <code>true</code>. Ranges use <code>gte</code> / <code>lte</code>.
      </InlineTip>

      {isLoading ? (
        <LoadingRows count={4} height="h-14" />
      ) : isError ? (
        <ErrorState message={error instanceof Error ? error.message : 'Failed to load defaults'} />
      ) : !data || data.length === 0 ? (
        <EmptyState
          icon={<Filter className="size-6" />}
          title="No defaults configured yet"
          description="Add one to auto-apply it on the Companies list."
          action={<AddButton />}
        />
      ) : (
        <div className="overflow-hidden rounded-lg border border-border/70 bg-card shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-muted/40">
                <tr className="border-b border-border/70">
                  <Th>Sort</Th>
                  <Th>Filter key</Th>
                  <Th>Label</Th>
                  <Th>Operator</Th>
                  <Th>Value (JSON)</Th>
                  <Th>Active</Th>
                  <Th className="text-right pr-4">Actions</Th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/70">
                {data.map((row) => <Row key={row.id} row={row} />)}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}

function Th({ children, className }: { children: React.ReactNode; className?: string }) {
  return (
    <th className={`px-3 py-2.5 text-left text-xs font-semibold uppercase tracking-wide text-muted-foreground ${className ?? ''}`}>
      {children}
    </th>
  )
}

function Row({ row }: { row: CompanyDefaultFilter }) {
  const update = useUpdateCompanyDefault(row.id)
  const remove = useDeleteCompanyDefault()
  const [editing, setEditing] = useState(false)
  const [label, setLabel] = useState(row.label)
  const [op, setOp] = useState<FilterOperator>(row.operator)
  const [value, setValue] = useState(row.value ?? '')
  const [sortOrder, setSortOrder] = useState<number>(row.sort_order)

  function save() {
    update.mutate(
      { filter_key: row.filter_key, label, operator: op, value: value || null, active: row.active, sort_order: sortOrder },
      { onSuccess: () => setEditing(false) },
    )
  }

  return (
    <tr className="hover:bg-muted/30">
      <td className="px-3 py-2 tabular-nums w-16">
        {editing ? (
          <Input type="number" className="h-8" value={sortOrder} onChange={(e) => setSortOrder(Number(e.target.value))} />
        ) : row.sort_order}
      </td>
      <td className="px-3 py-2 font-mono text-xs">{row.filter_key}</td>
      <td className="px-3 py-2">
        {editing ? <Input className="h-8" value={label} onChange={(e) => setLabel(e.target.value)} /> : row.label}
      </td>
      <td className="px-3 py-2">
        {editing ? (
          <Select className="h-8" value={op} onChange={(e) => setOp(e.target.value as FilterOperator)}>
            {['eq','ne','gte','lte','gt','lt','between','in','is_present','is_missing'].map((o) =>
              <option key={o} value={o}>{o}</option>)}
          </Select>
        ) : (
          <span className="font-mono text-xs">{row.operator}</span>
        )}
      </td>
      <td className="px-3 py-2 max-w-[280px]">
        {editing ? (
          <Input className="h-8 font-mono text-xs" value={value} onChange={(e) => setValue(e.target.value)} placeholder='e.g. 10 · true · "READY"' />
        ) : (
          <span className="font-mono text-xs truncate block">{row.value ?? '—'}</span>
        )}
      </td>
      <td className="px-3 py-2">
        <button
          type="button"
          onClick={() => update.mutate({
            filter_key: row.filter_key, label: row.label, operator: row.operator,
            value: row.value, active: !row.active, sort_order: row.sort_order,
          })}
          className={row.active ? 'text-emerald-600' : 'text-muted-foreground'}
          aria-label="toggle active"
        >
          {row.active ? '● Active' : '○ Off'}
        </button>
      </td>
      <td className="px-3 py-2 text-right pr-4">
        {editing ? (
          <div className="inline-flex gap-1">
            <Button size="xs" variant="outline" onClick={() => { setEditing(false); setLabel(row.label); setOp(row.operator); setValue(row.value ?? ''); setSortOrder(row.sort_order) }}>Cancel</Button>
            <Button size="xs" onClick={save} disabled={update.isPending}>Save</Button>
          </div>
        ) : (
          <div className="inline-flex gap-1">
            <Button size="xs" variant="outline" onClick={() => setEditing(true)}>Edit</Button>
            <Button size="xs" variant="ghost" onClick={() => {
              if (confirm('Remove this default filter?')) remove.mutate(row.id)
            }}>
              <Trash2 className="size-3.5" />
            </Button>
          </div>
        )}
      </td>
    </tr>
  )
}

function AddButton() {
  const [open, setOpen] = useState(false)
  const create = useCreateCompanyDefault()
  const first = FILTER_KEY_OPTIONS[0]!
  const [key, setKey] = useState(first.key)
  const [label, setLabel] = useState(first.label)
  const [op, setOp] = useState<FilterOperator>(first.operator)
  const [value, setValue] = useState('')

  function pickKey(k: string) {
    setKey(k)
    const preset = FILTER_KEY_OPTIONS.find((x) => x.key === k)
    if (preset) {
      setLabel(preset.label)
      setOp(preset.operator)
    }
  }

  return (
    <>
      <Button onClick={() => setOpen(true)}><Plus /> Add default</Button>
      {open ? (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-sm flex items-center justify-center p-4" onClick={() => setOpen(false)}>
          <div className="w-full max-w-lg rounded-xl bg-card border border-border/70 shadow-lg p-6 space-y-4" onClick={(e) => e.stopPropagation()}>
            <h2 className="text-lg font-semibold tracking-tight">Add default filter</h2>
            <div className="grid gap-3">
              <div>
                <Label>Filter</Label>
                <Select value={key} onChange={(e) => pickKey(e.target.value)}>
                  {FILTER_KEY_OPTIONS.map((f) => <option key={f.key} value={f.key}>{f.label} ({f.key})</option>)}
                </Select>
              </div>
              <div>
                <Label>Label</Label>
                <Input value={label} onChange={(e) => setLabel(e.target.value)} />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <Label>Operator</Label>
                  <Select value={op} onChange={(e) => setOp(e.target.value as FilterOperator)}>
                    {['eq','ne','gte','lte','gt','lt','between','in','is_present','is_missing'].map((o) =>
                      <option key={o} value={o}>{o}</option>)}
                  </Select>
                </div>
                <div>
                  <Label>Value (JSON)</Label>
                  <Input className="font-mono text-xs" value={value} onChange={(e) => setValue(e.target.value)} placeholder='e.g. 10, true, "READY"' />
                </div>
              </div>
            </div>
            <div className="flex justify-end gap-2">
              <Button variant="outline" onClick={() => setOpen(false)}>Cancel</Button>
              <Button onClick={() => {
                create.mutate({ filter_key: key, label, operator: op, value: value || null }, {
                  onSuccess: () => setOpen(false),
                })
              }}>Save</Button>
            </div>
          </div>
        </div>
      ) : null}
    </>
  )
}

import { Button } from '@/components/ui/button'
import { PageHeader } from '@/components/layout/PageHeader'
import { Plus } from 'lucide-react'
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useState } from 'react'
import { useCreateContactRole, useContactRoles, useUpdateContactRole } from '../hooks'

export function ContactRolesPage() {
  const { data, isLoading, isError } = useContactRoles(true)
  const create = useCreateContactRole()

  const [open, setOpen] = useState(false)
  const [key, setKey] = useState('')
  const [label, setLabel] = useState('')
  const [sort, setSort] = useState<number>(100)
  const [error, setError] = useState<string | null>(null)

  async function submit() {
    setError(null)
    try {
      await create.mutateAsync({ key: key.trim().toUpperCase(), label, sort_order: sort })
      setKey('')
      setLabel('')
      setSort(100)
      setOpen(false)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Save failed')
    }
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Contact Roles"
        description="Deactivate roles instead of deleting them. Deactivated roles disappear from new-contact dropdowns but still render on existing contacts."
        actions={<Button onClick={() => setOpen(true)}><Plus /> Add role</Button>}
      />

      {isLoading ? (
        <p className="text-sm text-muted-foreground py-4">Loading...</p>
      ) : isError ? (
        <p className="text-sm text-destructive py-4">Failed to load contact roles.</p>
      ) : (
        <div className="overflow-hidden rounded-xl border border-border bg-card shadow-card">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border bg-surface-1">
                  <th className="py-3 px-3 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Sort</th>
                  <th className="py-3 px-3 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Key</th>
                  <th className="py-3 px-3 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Label</th>
                  <th className="py-3 px-3 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Active</th>
                  <th className="py-3 px-3 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Used by</th>
                  <th className="py-3 px-3 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {data?.map((r) => <ContactRoleRow key={r.id} row={r} />)}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Add contact role</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="role-key" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Key</Label>
              <Input
                id="role-key"
                value={key}
                onChange={(e) => setKey(e.target.value.toUpperCase())}
                placeholder="e.g. QC_MANAGER"
              />
              <p className="text-[11px] text-muted-foreground">Uppercase letters, digits and underscores only.</p>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="role-label" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Label</Label>
              <Input id="role-label" value={label} onChange={(e) => setLabel(e.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="role-sort" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Sort order</Label>
              <Input
                id="role-sort"
                type="number"
                value={sort}
                onChange={(e) => setSort(Number(e.target.value))}
              />
            </div>
            {error ? <p className="text-sm font-medium text-destructive">{error}</p> : null}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setOpen(false)}>Cancel</Button>
            <Button onClick={submit} disabled={!key || !label}>Save</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}

function ContactRoleRow({ row }: { row: import('../types').ContactRoleResponse }) {
  const update = useUpdateContactRole(row.id)
  return (
    <tr className="hover:bg-accent/30 transition-colors duration-150 group/row">
      <td className="py-2.5 px-3 tabular-nums font-medium">{row.sort_order}</td>
      <td className="py-2.5 px-3 font-mono text-xs font-semibold">{row.key}</td>
      <td className="py-2.5 px-3">{row.label}</td>
      <td className="py-2.5 px-3">
        <button
          type="button"
          className={`inline-flex items-center gap-1.5 text-xs font-semibold transition-colors duration-200 ${row.active ? 'text-accent-emerald' : 'text-muted-foreground'}`}
          onClick={() => update.mutate({ active: !row.active })}
          disabled={update.isPending}
        >
          <span className={`inline-flex size-2.5 rounded-full ${row.active ? 'bg-accent-emerald' : 'bg-muted-foreground/30'}`} />
          {row.active ? 'Active' : 'Off'}
        </button>
      </td>
      <td className="py-2.5 px-3">
        <span className="inline-flex items-center rounded-md bg-surface-1 px-2 py-0.5 text-[11px] font-semibold border border-border tabular-nums">
          {row.usage_count}
        </span>
      </td>
      <td className="py-2.5 px-3 text-right"></td>
    </tr>
  )
}

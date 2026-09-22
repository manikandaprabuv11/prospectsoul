import { Button } from '@/components/ui/button'
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

/**
 * `/settings/contact-roles` — UI/UX Addendum §3. Admin-only, deactivate-only
 * per Domain Model Addendum Invariant 14.
 */
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
    <div className="p-6 space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold">Contact Roles</h1>
          <p className="text-sm text-muted-foreground">
            Deactivate roles instead of deleting them. Deactivated roles disappear from new-contact
            dropdowns but still render on existing contacts.
          </p>
        </div>
        <Button size="sm" onClick={() => setOpen(true)}>+ Add role</Button>
      </div>

      {isLoading ? (
        <p className="text-sm text-muted-foreground">Loading…</p>
      ) : isError ? (
        <p className="text-sm text-destructive">Failed to load contact roles.</p>
      ) : (
        <table className="w-full text-sm">
          <thead>
            <tr className="text-left border-b text-muted-foreground">
              <th className="py-2 px-2">Sort</th>
              <th className="py-2 px-2">Key</th>
              <th className="py-2 px-2">Label</th>
              <th className="py-2 px-2">Active</th>
              <th className="py-2 px-2">Used by</th>
              <th className="py-2 px-2"></th>
            </tr>
          </thead>
          <tbody>
            {data?.map((r) => <ContactRoleRow key={r.id} row={r} />)}
          </tbody>
        </table>
      )}

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Add contact role</DialogTitle>
          </DialogHeader>
          <div className="space-y-3">
            <div>
              <Label htmlFor="role-key">Key</Label>
              <Input
                id="role-key"
                value={key}
                onChange={(e) => setKey(e.target.value.toUpperCase())}
                placeholder="e.g. QC_MANAGER"
              />
              <p className="text-xs text-muted-foreground mt-1">Uppercase letters, digits and underscores only.</p>
            </div>
            <div>
              <Label htmlFor="role-label">Label</Label>
              <Input id="role-label" value={label} onChange={(e) => setLabel(e.target.value)} />
            </div>
            <div>
              <Label htmlFor="role-sort">Sort order</Label>
              <Input
                id="role-sort"
                type="number"
                value={sort}
                onChange={(e) => setSort(Number(e.target.value))}
              />
            </div>
            {error ? <p className="text-sm text-destructive">{error}</p> : null}
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
    <tr className="border-b hover:bg-accent/30">
      <td className="py-1.5 px-2">{row.sort_order}</td>
      <td className="py-1.5 px-2 font-mono text-xs">{row.key}</td>
      <td className="py-1.5 px-2">{row.label}</td>
      <td className="py-1.5 px-2">
        <button
          type="button"
          className={row.active ? 'text-emerald-600' : 'text-muted-foreground'}
          onClick={() => update.mutate({ active: !row.active })}
          disabled={update.isPending}
        >
          {row.active ? '●' : '○'}
        </button>
      </td>
      <td className="py-1.5 px-2">{row.usage_count}</td>
      <td className="py-1.5 px-2 text-right"></td>
    </tr>
  )
}

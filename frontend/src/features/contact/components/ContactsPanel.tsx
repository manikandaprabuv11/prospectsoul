import { Button } from '@/components/ui/button'
import { useContactRoles } from '@/features/contactrole/hooks'
import { useState } from 'react'
import {
  useCompanyContacts,
  useCreateContact,
  useMakeContactPrimary,
} from '../hooks'

interface Props { companyId: string }

/**
 * Multi-contact list (UI/UX Addendum §7.1). Primary is a star; every other
 * contact shows a "Make primary" button. Role is a first-class filter
 * dimension (ADR-0001), rendered as a chip alongside the name.
 */
export function ContactsPanel({ companyId }: Props) {
  const { data: contacts, isLoading } = useCompanyContacts(companyId)
  const { data: roles } = useContactRoles(false)
  const makePrimary = useMakeContactPrimary(companyId)
  const create = useCreateContact(companyId)
  const [open, setOpen] = useState(false)
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [email, setEmail] = useState('')
  const [roleId, setRoleId] = useState<string>('')
  const [isPrimary, setIsPrimary] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function submit() {
    setError(null)
    if (!roleId) { setError('Role is required'); return }
    try {
      await create.mutateAsync({ name, phone, email, role_id: roleId, is_primary: isPrimary })
      setName(''); setPhone(''); setEmail(''); setRoleId(''); setIsPrimary(false)
      setOpen(false)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Save failed')
    }
  }

  return (
    <section className="space-y-3">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold">
          Contacts <span className="text-muted-foreground font-normal">({contacts?.length ?? 0})</span>
        </h3>
        <Button size="sm" onClick={() => setOpen((o) => !o)}>+ Add contact</Button>
      </div>

      {isLoading ? (
        <p className="text-sm text-muted-foreground">Loading…</p>
      ) : contacts && contacts.length === 0 ? (
        <p className="text-sm text-muted-foreground">No contacts yet. Add one to make it primary.</p>
      ) : (
        <ul className="space-y-2">
          {contacts?.map((c) => (
            <li key={c.id} className="rounded-md border p-3">
              <div className="flex items-center justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-medium">{c.name}</span>
                    {c.role_label ? (
                      <span className="text-xs rounded-full border px-2 py-0.5 text-muted-foreground">
                        {c.role_label}
                      </span>
                    ) : null}
                    {c.is_primary ? <span className="text-amber-500">★ Primary</span> : null}
                  </div>
                  <div className="text-xs text-muted-foreground mt-1">
                    {c.phone ?? '—'} · {c.email ?? '—'}
                  </div>
                </div>
                {!c.is_primary ? (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => makePrimary.mutate(c.id)}
                    disabled={makePrimary.isPending}
                  >
                    Make primary
                  </Button>
                ) : null}
              </div>
            </li>
          ))}
        </ul>
      )}

      {open ? (
        <div className="rounded-md border p-3 space-y-2">
          <input
            className="w-full rounded border px-2 py-1 text-sm"
            placeholder="Name"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
          <input
            className="w-full rounded border px-2 py-1 text-sm"
            placeholder="Phone"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
          />
          <input
            className="w-full rounded border px-2 py-1 text-sm"
            placeholder="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <select
            className="w-full rounded border px-2 py-1 text-sm"
            value={roleId}
            onChange={(e) => setRoleId(e.target.value)}
          >
            <option value="">Select role…</option>
            {roles?.map((r) => (
              <option key={r.id} value={r.id}>{r.label}</option>
            ))}
          </select>
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" checked={isPrimary} onChange={(e) => setIsPrimary(e.target.checked)} />
            Set as primary contact
          </label>
          {error ? <p className="text-sm text-destructive">{error}</p> : null}
          <div className="flex justify-end gap-2">
            <Button variant="outline" size="sm" onClick={() => setOpen(false)}>Cancel</Button>
            <Button size="sm" onClick={submit} disabled={!name || !roleId}>Save</Button>
          </div>
        </div>
      ) : null}
    </section>
  )
}

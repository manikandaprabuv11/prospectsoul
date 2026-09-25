import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { useContactRoles } from '@/features/contactrole/hooks'
import { Star, UserPlus, X } from 'lucide-react'
import { useState } from 'react'
import {
  useCompanyContacts,
  useCreateContact,
  useMakeContactPrimary,
} from '../hooks'

interface Props { companyId: string }

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
        <Button size="sm" onClick={() => setOpen((o) => !o)}>
          <UserPlus className="size-3.5" /> Add contact
        </Button>
      </div>

      {isLoading ? (
        <div className="space-y-2">
          {[0, 1].map((i) => (
            <div key={i} className="h-16 animate-pulse rounded-xl bg-surface-1" />
          ))}
        </div>
      ) : contacts && contacts.length === 0 ? (
        <div className="rounded-xl border border-dashed border-border bg-surface-1/50 py-8 text-center">
          <p className="text-sm text-muted-foreground">No contacts yet. Add one to make it primary.</p>
        </div>
      ) : (
        <ul className="space-y-2">
          {contacts?.map((c) => (
            <li key={c.id} className="group rounded-xl border border-border bg-card p-3 transition-shadow hover:shadow-card">
              <div className="flex items-center justify-between">
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="font-semibold text-sm">{c.name}</span>
                    {c.role_label ? (
                      <span className="rounded-md border border-border bg-surface-1 px-2 py-0.5 text-[11px] font-semibold text-muted-foreground">
                        {c.role_label}
                      </span>
                    ) : null}
                    {c.is_primary ? (
                      <span className="inline-flex items-center gap-1 text-accent-amber text-xs font-semibold">
                        <Star className="size-3 fill-accent-amber" /> Primary
                      </span>
                    ) : null}
                  </div>
                  <div className="text-xs text-muted-foreground mt-1">
                    {c.phone ?? '—'} · {c.email ?? '—'}
                  </div>
                </div>
                {!c.is_primary ? (
                  <Button
                    variant="ghost"
                    size="sm"
                    className="opacity-60 group-hover:opacity-100 transition-opacity"
                    onClick={() => makePrimary.mutate(c.id)}
                    disabled={makePrimary.isPending}
                  >
                    <Star className="size-3.5" /> Make primary
                  </Button>
                ) : null}
              </div>
            </li>
          ))}
        </ul>
      )}

      {open ? (
        <div className="animate-slide-up rounded-xl border border-border bg-card p-4 shadow-card space-y-3">
          <div className="flex items-center justify-between">
            <h4 className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">New contact</h4>
            <button
              type="button"
              className="rounded-lg p-1 text-muted-foreground hover:text-foreground hover:bg-accent transition-colors"
              onClick={() => setOpen(false)}
            >
              <X className="size-3.5" />
            </button>
          </div>
          <Input placeholder="Name" value={name} onChange={(e) => setName(e.target.value)} />
          <Input placeholder="Phone" value={phone} onChange={(e) => setPhone(e.target.value)} />
          <Input placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} />
          <Select value={roleId} onChange={(e) => setRoleId(e.target.value)}>
            <option value="">Select role…</option>
            {roles?.map((r) => (
              <option key={r.id} value={r.id}>{r.label}</option>
            ))}
          </Select>
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" checked={isPrimary} onChange={(e) => setIsPrimary(e.target.checked)} className="size-4 rounded accent-primary" />
            Set as primary contact
          </label>
          {error ? (
            <div className="rounded-lg border border-destructive/30 bg-destructive/5 px-3 py-2 text-sm font-medium text-destructive">{error}</div>
          ) : null}
          <div className="flex justify-end gap-2 pt-1">
            <Button variant="outline" size="sm" onClick={() => setOpen(false)}>Cancel</Button>
            <Button size="sm" onClick={submit} disabled={!name || !roleId}>Save</Button>
          </div>
        </div>
      ) : null}
    </section>
  )
}

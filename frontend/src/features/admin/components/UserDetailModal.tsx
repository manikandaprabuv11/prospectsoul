import { useState } from 'react'
import type { FormEvent } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import { X } from 'lucide-react'
import type { UserResponse } from '../types/user'

const ROLES = [
  { value: 'PS_ANALYST', label: 'Research Analyst' },
  { value: 'PS_SALES_LEAD', label: 'Sales Lead' },
  { value: 'PS_ADMIN', label: 'Administrator' },
  { value: 'PS_VIEWER', label: 'Viewer' },
  { value: 'PS_COO', label: 'COO' },
]

interface UserFormData {
  username: string
  fullName: string
  email: string
  password: string
  role: string
}

interface UserDetailModalProps {
  user: UserResponse | null
  onClose: () => void
  onSave: (data: UserFormData) => Promise<void>
  saving: boolean
  error: string | null
}

export function UserDetailModal({ user, onClose, onSave, saving, error }: UserDetailModalProps) {
  const isEdit = user !== null

  const [form, setForm] = useState<UserFormData>({
    username: user?.username ?? '',
    fullName: user?.full_name ?? '',
    email: user?.email ?? '',
    password: '',
    role: user?.role ?? '',
  })

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    await onSave(form)
  }

  const canSubmit = isEdit
    ? form.fullName.trim() && form.email.trim() && form.role
    : form.username.trim() && form.fullName.trim() && form.email.trim() && form.password.length >= 8 && form.role

  return (
    <div
      className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4"
      onClick={(e) => { if (e.target === e.currentTarget) onClose() }}
    >
      <div className="w-full max-w-md rounded-2xl border border-border bg-card shadow-2xl animate-scale-in">
        <div className="flex items-center justify-between px-6 py-4 border-b border-border">
          <h3 className="text-lg font-bold tracking-tight">{isEdit ? 'Edit user' : 'Add new user'}</h3>
          <button
            type="button"
            className="rounded-lg p-1.5 text-muted-foreground hover:text-foreground hover:bg-accent transition-colors duration-200"
            onClick={onClose}
          >
            <X className="size-4" />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="px-6 py-5 space-y-4">
            {error && (
              <div className="rounded-xl border border-destructive/30 bg-destructive/5 px-4 py-3 text-sm font-medium text-destructive">
                {error}
              </div>
            )}

            <div className="space-y-1.5">
              <Label htmlFor="um-fullname" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Full name</Label>
              <Input
                id="um-fullname"
                placeholder="Kavitha Sundaram"
                value={form.fullName}
                onChange={(e) => setForm({ ...form, fullName: e.target.value })}
                disabled={saving}
              />
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="um-email" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Email address</Label>
              <Input
                id="um-email"
                type="email"
                placeholder="kavitha@vyoog.com"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                disabled={saving}
              />
            </div>

            {!isEdit && (
              <>
                <div className="space-y-1.5">
                  <Label htmlFor="um-username" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Username</Label>
                  <Input
                    id="um-username"
                    placeholder="kavitha"
                    value={form.username}
                    onChange={(e) => setForm({ ...form, username: e.target.value })}
                    disabled={saving}
                  />
                  <p className="text-[11px] text-muted-foreground">Used for login. Cannot be changed later.</p>
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="um-password" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Temporary password</Label>
                  <Input
                    id="um-password"
                    type="password"
                    placeholder="Minimum 8 characters"
                    value={form.password}
                    onChange={(e) => setForm({ ...form, password: e.target.value })}
                    disabled={saving}
                  />
                  <p className="text-[11px] text-muted-foreground">User will be prompted to change on first login.</p>
                </div>
              </>
            )}

            <div className="space-y-1.5">
              <Label htmlFor="um-role" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Role</Label>
              <Select
                id="um-role"
                value={form.role}
                onChange={(e) => setForm({ ...form, role: e.target.value })}
                disabled={saving}
              >
                <option value="">Select a role...</option>
                {ROLES.map((r) => (
                  <option key={r.value} value={r.value}>{r.label}</option>
                ))}
              </Select>
            </div>
          </div>

          <div className="flex justify-end gap-2 px-6 py-4 border-t border-border">
            <Button variant="outline" type="button" onClick={onClose} disabled={saving}>
              Cancel
            </Button>
            <Button type="submit" disabled={saving || !canSubmit}>
              {saving ? 'Saving...' : isEdit ? 'Save changes' : 'Create user'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}

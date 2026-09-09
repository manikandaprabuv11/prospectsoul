import { useState } from 'react'
import type { FormEvent } from 'react'
import type { UserResponse } from '../types/user'
import './UserDetailModal.css'

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
    <div className="um-modal-overlay" onClick={(e) => { if (e.target === e.currentTarget) onClose() }}>
      <div className="um-modal">
        <div className="um-modal-header">
          <h3>{isEdit ? 'Edit user' : 'Add new user'}</h3>
          <button type="button" className="um-modal-close" onClick={onClose}>
            <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
              <path d="M4 4l10 10M14 4L4 14" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
            </svg>
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="um-modal-body">
            {error && (
              <div className="um-modal-error">{error}</div>
            )}

            <div className="um-modal-field">
              <label htmlFor="um-fullname">Full name</label>
              <input
                type="text"
                id="um-fullname"
                placeholder="Kavitha Sundaram"
                value={form.fullName}
                onChange={(e) => setForm({ ...form, fullName: e.target.value })}
                disabled={saving}
              />
            </div>

            <div className="um-modal-field">
              <label htmlFor="um-email">Email address</label>
              <input
                type="email"
                id="um-email"
                placeholder="kavitha@vyoog.com"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                disabled={saving}
              />
            </div>

            {!isEdit && (
              <>
                <div className="um-modal-field">
                  <label htmlFor="um-username">Username</label>
                  <input
                    type="text"
                    id="um-username"
                    placeholder="kavitha"
                    value={form.username}
                    onChange={(e) => setForm({ ...form, username: e.target.value })}
                    disabled={saving}
                  />
                  <div className="um-hint">Used for login. Cannot be changed later.</div>
                </div>

                <div className="um-modal-field">
                  <label htmlFor="um-password">Temporary password</label>
                  <input
                    type="password"
                    id="um-password"
                    placeholder="Minimum 8 characters"
                    value={form.password}
                    onChange={(e) => setForm({ ...form, password: e.target.value })}
                    disabled={saving}
                  />
                  <div className="um-hint">User will be prompted to change on first login.</div>
                </div>
              </>
            )}

            <div className="um-modal-field">
              <label htmlFor="um-role">Role</label>
              <select
                id="um-role"
                value={form.role}
                onChange={(e) => setForm({ ...form, role: e.target.value })}
                disabled={saving}
              >
                <option value="">Select a role…</option>
                {ROLES.map((r) => (
                  <option key={r.value} value={r.value}>{r.label}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="um-modal-footer">
            <button type="button" className="um-btn-cancel" onClick={onClose} disabled={saving}>
              Cancel
            </button>
            <button type="submit" className="um-btn-save" disabled={saving || !canSubmit}>
              {saving ? 'Saving…' : isEdit ? 'Save changes' : 'Create user'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

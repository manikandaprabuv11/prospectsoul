import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError } from '@/api/client'
import { Button } from '@/components/ui/button'
import { Select } from '@/components/ui/select'
import { Loader2, X } from 'lucide-react'
import { rolesApi } from '../api/rolesApi'
import { usersApi } from '../api/usersApi'
import type { RoleDetailResponse } from '../types/role'
import type { UserResponse } from '../types/user'
import { PermissionTag } from './PermissionTag'
import { RoleBadge } from './RoleBadge'
import { StatusBadge } from './StatusBadge'

interface RoleDetailDrawerProps {
  roleId: string
  onClose: () => void
  onChanged: () => void
}

export function RoleDetailDrawer({ roleId, onClose, onChanged }: RoleDetailDrawerProps) {
  const [detail, setDetail] = useState<RoleDetailResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [editing, setEditing] = useState(false)
  const [editDesc, setEditDesc] = useState('')
  const [saving, setSaving] = useState(false)

  const [allUsers, setAllUsers] = useState<UserResponse[]>([])
  const [assignUserId, setAssignUserId] = useState('')
  const [assigning, setAssigning] = useState(false)
  const [assignError, setAssignError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    rolesApi.getById(roleId)
      .then((r) => { if (!cancelled) { setDetail(r); setEditDesc(r.description) } })
      .catch((e) => { if (!cancelled) setError(e instanceof ApiError ? e.message : 'Failed to load role') })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [roleId])

  useEffect(() => {
    usersApi.list({ size: 100 }).then((r) => setAllUsers(r.content)).catch(() => {})
  }, [])

  const handleSaveDescription = async (e: FormEvent) => {
    e.preventDefault()
    if (!detail) return
    setSaving(true)
    try {
      await rolesApi.update(detail.id, { description: editDesc })
      setDetail({ ...detail, description: editDesc })
      setEditing(false)
      onChanged()
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Failed to save')
    } finally {
      setSaving(false)
    }
  }

  const handleAssign = async () => {
    if (!assignUserId || !detail) return
    setAssigning(true)
    setAssignError(null)
    try {
      await rolesApi.assignUser(detail.id, assignUserId)
      const updated = await rolesApi.getById(detail.id)
      setDetail(updated)
      setAssignUserId('')
      onChanged()
    } catch (e) {
      setAssignError(e instanceof ApiError ? (e.problem?.detail ?? e.message) : 'Failed to assign')
    } finally {
      setAssigning(false)
    }
  }

  const handleUnassign = async (userId: string) => {
    if (!detail) return
    try {
      await rolesApi.unassignUser(detail.id, userId)
      const updated = await rolesApi.getById(detail.id)
      setDetail(updated)
      onChanged()
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Failed to unassign')
    }
  }

  const assignableUsers = allUsers.filter(
    (u) => u.active && !detail?.assigned_users.some((au) => au.id === u.id),
  )

  return (
    <div
      className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex justify-end"
      onClick={(e) => { if (e.target === e.currentTarget) onClose() }}
    >
      <div className="w-full max-w-lg bg-card border-l border-border shadow-2xl flex flex-col animate-slide-up">
        <div className="flex items-center justify-between px-6 py-4 border-b border-border shrink-0">
          <h3 className="text-lg font-bold tracking-tight">{detail?.display_name ?? 'Role Detail'}</h3>
          <button
            type="button"
            className="rounded-lg p-1.5 text-muted-foreground hover:text-foreground hover:bg-accent transition-colors duration-200"
            onClick={onClose}
          >
            <X className="size-4" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto px-6 py-5 space-y-6">
          {loading && (
            <div className="flex items-center justify-center py-16">
              <Loader2 className="size-8 text-primary animate-spin" />
            </div>
          )}

          {error && (
            <div className="rounded-xl border border-destructive/30 bg-destructive/5 px-4 py-3 text-sm font-medium text-destructive">
              {error}
            </div>
          )}

          {detail && !loading && (
            <>
              <div className="flex items-center gap-3">
                <RoleBadge role={detail.name} displayName={detail.display_name} />
                <span className="text-sm text-muted-foreground">{detail.user_count} user{detail.user_count !== 1 ? 's' : ''}</span>
              </div>

              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <h4 className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Description</h4>
                  {!editing && (
                    <Button variant="ghost" size="xs" onClick={() => setEditing(true)}>Edit</Button>
                  )}
                </div>
                {editing ? (
                  <form onSubmit={handleSaveDescription} className="space-y-3">
                    <textarea
                      className="w-full rounded-lg border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40 resize-none"
                      value={editDesc}
                      onChange={(e) => setEditDesc(e.target.value)}
                      rows={3}
                      disabled={saving}
                    />
                    <div className="flex gap-2">
                      <Button variant="outline" size="sm" type="button" onClick={() => { setEditing(false); setEditDesc(detail.description) }} disabled={saving}>Cancel</Button>
                      <Button size="sm" type="submit" disabled={saving}>{saving ? 'Saving...' : 'Save'}</Button>
                    </div>
                  </form>
                ) : (
                  <p className="text-sm text-foreground leading-relaxed">{detail.description || 'No description'}</p>
                )}
              </div>

              <div className="space-y-3">
                <h4 className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Permissions</h4>
                <div className="flex flex-wrap gap-1.5">
                  {detail.permissions.length > 0 ? (
                    detail.permissions.map((p) => <PermissionTag key={p} permission={p} />)
                  ) : (
                    <span className="text-sm text-muted-foreground">No permissions defined</span>
                  )}
                </div>
              </div>

              <div className="space-y-3">
                <h4 className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
                  Assigned users ({detail.assigned_users.length})
                </h4>

                {assignError && (
                  <div className="rounded-xl border border-destructive/30 bg-destructive/5 px-3 py-2 text-xs font-medium text-destructive">
                    {assignError}
                  </div>
                )}

                <div className="flex gap-2">
                  <Select
                    value={assignUserId}
                    onChange={(e) => setAssignUserId(e.target.value)}
                    disabled={assigning}
                    className="flex-1"
                  >
                    <option value="">Select user to assign...</option>
                    {assignableUsers.map((u) => (
                      <option key={u.id} value={u.id}>{u.full_name} ({u.username})</option>
                    ))}
                  </Select>
                  <Button
                    size="sm"
                    onClick={handleAssign}
                    disabled={!assignUserId || assigning}
                  >
                    {assigning ? 'Adding...' : 'Add'}
                  </Button>
                </div>

                <div className="space-y-1.5">
                  {detail.assigned_users.length === 0 ? (
                    <div className="rounded-xl border border-border bg-surface-1/50 py-8 text-center text-sm text-muted-foreground">
                      No users assigned to this role
                    </div>
                  ) : (
                    detail.assigned_users.map((u) => (
                      <div key={u.id} className="flex items-center justify-between rounded-xl border border-border bg-card p-3 hover:shadow-card transition-shadow duration-200">
                        <div className="min-w-0 flex-1">
                          <div className="font-semibold text-sm truncate">{u.full_name}</div>
                          <div className="text-xs text-muted-foreground truncate">{u.email}</div>
                        </div>
                        <div className="flex items-center gap-3 shrink-0">
                          <StatusBadge active={u.active} />
                          <button
                            type="button"
                            className="rounded-lg p-1.5 text-muted-foreground hover:text-accent-rose hover:bg-accent-rose/10 transition-colors duration-200"
                            title="Remove from role"
                            onClick={() => handleUnassign(u.id)}
                          >
                            <X className="size-3.5" />
                          </button>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  )
}

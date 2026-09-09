import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiError } from '@/api/client'
import { rolesApi } from '../api/rolesApi'
import { usersApi } from '../api/usersApi'
import type { RoleDetailResponse } from '../types/role'
import type { UserResponse } from '../types/user'
import { PermissionTag } from './PermissionTag'
import { RoleBadge } from './RoleBadge'
import { StatusBadge } from './StatusBadge'
import './RoleDetailDrawer.css'

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
    <div className="rd-overlay" onClick={(e) => { if (e.target === e.currentTarget) onClose() }}>
      <div className="rd-drawer">
        <div className="rd-header">
          <h3>{detail?.display_name ?? 'Role Detail'}</h3>
          <button type="button" className="rd-close" onClick={onClose}>
            <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
              <path d="M4 4l10 10M14 4L4 14" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
            </svg>
          </button>
        </div>

        <div className="rd-body">
          {loading && (
            <div className="rd-loading">
              <div className="rd-spinner" />
            </div>
          )}

          {error && <div className="rd-error">{error}</div>}

          {detail && !loading && (
            <>
              {/* Role info */}
              <div className="rd-section">
                <div className="rd-meta">
                  <RoleBadge role={detail.name} displayName={detail.display_name} />
                  <span className="rd-user-count">{detail.user_count} user{detail.user_count !== 1 ? 's' : ''}</span>
                </div>
              </div>

              {/* Description */}
              <div className="rd-section">
                <div className="rd-section-header">
                  <h4>Description</h4>
                  {!editing && (
                    <button type="button" className="rd-edit-btn" onClick={() => setEditing(true)}>Edit</button>
                  )}
                </div>
                {editing ? (
                  <form onSubmit={handleSaveDescription}>
                    <textarea
                      className="rd-textarea"
                      value={editDesc}
                      onChange={(e) => setEditDesc(e.target.value)}
                      rows={3}
                      disabled={saving}
                    />
                    <div className="rd-edit-actions">
                      <button type="button" className="rd-btn-secondary" onClick={() => { setEditing(false); setEditDesc(detail.description) }} disabled={saving}>Cancel</button>
                      <button type="submit" className="rd-btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Save'}</button>
                    </div>
                  </form>
                ) : (
                  <p className="rd-description">{detail.description || 'No description'}</p>
                )}
              </div>

              {/* Permissions */}
              <div className="rd-section">
                <h4>Permissions</h4>
                <div className="rd-perm-tags">
                  {detail.permissions.length > 0 ? (
                    detail.permissions.map((p) => <PermissionTag key={p} permission={p} />)
                  ) : (
                    <span className="rd-empty-text">No permissions defined</span>
                  )}
                </div>
              </div>

              {/* Assigned users */}
              <div className="rd-section">
                <h4>Assigned users ({detail.assigned_users.length})</h4>

                {assignError && <div className="rd-error" style={{ marginBottom: 12 }}>{assignError}</div>}

                <div className="rd-assign-row">
                  <select
                    className="rd-select"
                    value={assignUserId}
                    onChange={(e) => setAssignUserId(e.target.value)}
                    disabled={assigning}
                  >
                    <option value="">Select user to assign…</option>
                    {assignableUsers.map((u) => (
                      <option key={u.id} value={u.id}>{u.full_name} ({u.username})</option>
                    ))}
                  </select>
                  <button
                    type="button"
                    className="rd-btn-primary"
                    onClick={handleAssign}
                    disabled={!assignUserId || assigning}
                  >
                    {assigning ? 'Adding…' : 'Add'}
                  </button>
                </div>

                <div className="rd-user-list">
                  {detail.assigned_users.length === 0 ? (
                    <div className="rd-empty-text">No users assigned to this role</div>
                  ) : (
                    detail.assigned_users.map((u) => (
                      <div key={u.id} className="rd-user-row">
                        <div className="rd-user-info">
                          <div className="rd-user-name">{u.full_name}</div>
                          <div className="rd-user-email">{u.email}</div>
                        </div>
                        <StatusBadge active={u.active} />
                        <button
                          type="button"
                          className="rd-remove-btn"
                          title="Remove from role"
                          onClick={() => handleUnassign(u.id)}
                        >
                          <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                            <path d="M3 3l8 8M11 3l-8 8" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round"/>
                          </svg>
                        </button>
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

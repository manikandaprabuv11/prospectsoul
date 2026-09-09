import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { ApiError } from '@/api/client'
import { rolesApi } from '../api/rolesApi'
import { PermissionTag } from '../components/PermissionTag'
import { RoleDetailDrawer } from '../components/RoleDetailDrawer'
import type { RoleResponse } from '../types/role'
import './RolesPage.css'

const ROLE_ICONS: Record<string, React.ReactNode> = {
  PS_ANALYST: (
    <svg viewBox="0 0 20 20" fill="none"><circle cx="10" cy="6" r="3" stroke="currentColor" strokeWidth="1.5"/><path d="M3 17c0-3 3-5 7-5s7 2 7 5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/></svg>
  ),
  PS_SALES_LEAD: (
    <svg viewBox="0 0 20 20" fill="none"><path d="M10 2l2 6h6l-5 3.5 2 6.5-5-4-5 4 2-6.5L2 8h6l2-6z" stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round"/></svg>
  ),
  PS_ADMIN: (
    <svg viewBox="0 0 20 20" fill="none"><path d="M10 1l2.5 5H18l-4.5 3.5 1.8 5.5L10 12l-5.3 3 1.8-5.5L2 6h5.5L10 1z" stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round"/></svg>
  ),
  PS_VIEWER: (
    <svg viewBox="0 0 20 20" fill="none"><path d="M1.5 10s3.5-6 8.5-6 8.5 6 8.5 6-3.5 6-8.5 6-8.5-6-8.5-6z" stroke="currentColor" strokeWidth="1.5"/><circle cx="10" cy="10" r="2.5" stroke="currentColor" strokeWidth="1.5"/></svg>
  ),
  PS_COO: (
    <svg viewBox="0 0 20 20" fill="none"><rect x="2" y="4" width="16" height="12" rx="2" stroke="currentColor" strokeWidth="1.5"/><path d="M6 8h8M6 12h5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/></svg>
  ),
}

const UserCountIcon = () => (
  <svg viewBox="0 0 16 16" fill="none">
    <circle cx="8" cy="5" r="2.5" stroke="currentColor" strokeWidth="1.2"/>
    <path d="M2.5 14c0-2.5 2.5-4 5.5-4s5.5 1.5 5.5 4" stroke="currentColor" strokeWidth="1.2"/>
  </svg>
)

export function RolesPage() {
  const [roles, setRoles] = useState<RoleResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [selectedRoleId, setSelectedRoleId] = useState<string | null>(null)
  const [refreshKey, setRefreshKey] = useState(0)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    rolesApi.list()
      .then((r) => { if (!cancelled) { setRoles(r); setError(null) } })
      .catch((e) => { if (!cancelled) setError(e instanceof ApiError ? e.message : 'Failed to load roles') })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [refreshKey])

  const refetch = () => setRefreshKey((k) => k + 1)

  return (
    <div className="roles-page">
      <div className="page-header">
        <h2>Users & Roles</h2>
      </div>

      <div className="tabs">
        <Link to="/settings/users" className="tab-btn">
          Users
        </Link>
        <button type="button" className="tab-btn active">
          Roles <span className="tab-count">{roles.length}</span>
        </button>
      </div>

      {error && <div className="page-error">{error}</div>}

      {/* Role cards grid */}
      <div className="roles-grid">
        {loading ? (
          Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="skeleton-card">
              <div className="skeleton-bar" style={{ width: 40, height: 40, borderRadius: 10, marginBottom: 12 }} />
              <div className="skeleton-bar" style={{ width: 140, marginBottom: 8 }} />
              <div className="skeleton-bar" style={{ width: '100%', marginBottom: 6 }} />
              <div className="skeleton-bar" style={{ width: '80%', marginBottom: 14 }} />
              <div style={{ display: 'flex', gap: 5 }}>
                <div className="skeleton-bar" style={{ width: 50 }} />
                <div className="skeleton-bar" style={{ width: 60 }} />
                <div className="skeleton-bar" style={{ width: 40 }} />
              </div>
            </div>
          ))
        ) : (
          roles.map((role) => (
            <div
              key={role.id}
              className="role-card"
              onClick={() => setSelectedRoleId(role.id)}
            >
              <div className="role-card-header">
                <div className={`role-card-icon ${role.name}`}>
                  {ROLE_ICONS[role.name] ?? <UserCountIcon />}
                </div>
                <div className="user-count">
                  <UserCountIcon />
                  {role.user_count} user{role.user_count !== 1 ? 's' : ''}
                </div>
              </div>
              <h3>{role.display_name}</h3>
              <p>{role.description || 'No description'}</p>
              <div className="perm-tags">
                {role.permissions.map((p) => (
                  <PermissionTag key={p} permission={p} />
                ))}
              </div>
            </div>
          ))
        )}
      </div>

      {selectedRoleId && (
        <RoleDetailDrawer
          roleId={selectedRoleId}
          onClose={() => setSelectedRoleId(null)}
          onChanged={refetch}
        />
      )}
    </div>
  )
}

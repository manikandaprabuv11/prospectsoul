import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { ApiError } from '@/api/client'
import { usersApi } from '../api/usersApi'
import type { UserListParams } from '../api/usersApi'
import { RoleBadge } from '../components/RoleBadge'
import { StatusBadge } from '../components/StatusBadge'
import { UserDetailModal } from '../components/UserDetailModal'
import type { UserResponse, UsersPageResponse } from '../types/user'
import './UsersPage.css'

const ROLE_FILTERS = [
  { value: '', label: 'All roles' },
  { value: 'PS_ANALYST', label: 'Analyst' },
  { value: 'PS_SALES_LEAD', label: 'Sales Lead' },
  { value: 'PS_ADMIN', label: 'Admin' },
  { value: 'PS_VIEWER', label: 'Viewer' },
  { value: 'PS_COO', label: 'COO' },
]

const STATUS_FILTERS = [
  { value: undefined as boolean | undefined, label: 'All' },
  { value: true as boolean | undefined, label: 'Active' },
  { value: false as boolean | undefined, label: 'Inactive' },
]

function userInitials(fullName: string, username: string): string {
  if (fullName.trim()) {
    return fullName
      .split(/\s+/)
      .slice(0, 2)
      .map((w) => w[0]?.toUpperCase() ?? '')
      .join('')
  }
  return (username[0] ?? 'U').toUpperCase()
}

function formatDate(iso: string | null): string {
  if (!iso) return '—'
  const d = new Date(iso)
  const now = Date.now()
  const diff = now - d.getTime()
  if (diff < 60_000) return 'Just now'
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)} min ago`
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)} hours ago`
  if (diff < 604_800_000) return `${Math.floor(diff / 86_400_000)} days ago`
  return d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
}

function formatCreatedDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
}

type FetchState =
  | { status: 'loading' }
  | { status: 'success'; data: UsersPageResponse }
  | { status: 'error'; message: string }

export function UsersPage() {
  const [fetchState, setFetchState] = useState<FetchState>({ status: 'loading' })

  const [search, setSearch] = useState('')
  const [roleFilter, setRoleFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState<boolean | undefined>(undefined)
  const [page, setPage] = useState(0)

  const [modalUser, setModalUser] = useState<UserResponse | null | undefined>(undefined)
  const [modalSaving, setModalSaving] = useState(false)
  const [modalError, setModalError] = useState<string | null>(null)
  const [refreshKey, setRefreshKey] = useState(0)

  useEffect(() => {
    let cancelled = false
    setFetchState({ status: 'loading' })

    const params: UserListParams = {
      q: search || undefined,
      role: roleFilter || undefined,
      active: statusFilter,
      page,
      size: 25,
    }
    usersApi.list(params)
      .then((result) => { if (!cancelled) setFetchState({ status: 'success', data: result }) })
      .catch((e) => { if (!cancelled) setFetchState({ status: 'error', message: e instanceof ApiError ? e.message : 'Failed to load users' }) })

    return () => { cancelled = true }
  }, [search, roleFilter, statusFilter, page, refreshKey])

  const refetch = () => setRefreshKey((k) => k + 1)

  const loading = fetchState.status === 'loading'
  const error = fetchState.status === 'error' ? fetchState.message : null
  const data = fetchState.status === 'success' ? fetchState.data : null

  const handleSearch = (value: string) => {
    setSearch(value)
    setPage(0)
  }

  const handleRoleFilter = (value: string) => {
    setRoleFilter(value)
    setPage(0)
  }

  const handleStatusFilter = (value: boolean | undefined) => {
    setStatusFilter(value)
    setPage(0)
  }

  const openCreateModal = () => {
    setModalUser(null)
    setModalError(null)
  }

  const openEditModal = (user: UserResponse) => {
    setModalUser(user)
    setModalError(null)
  }

  const closeModal = () => {
    setModalUser(undefined)
    setModalError(null)
  }

  const handleSave = async (formData: { username: string; fullName: string; email: string; password: string; role: string }) => {
    setModalSaving(true)
    setModalError(null)
    try {
      if (modalUser === null) {
        await usersApi.create({
          username: formData.username,
          full_name: formData.fullName,
          email: formData.email,
          password: formData.password,
          role: formData.role,
        })
      } else if (modalUser) {
        await usersApi.update(modalUser.id, {
          full_name: formData.fullName,
          email: formData.email,
          role: formData.role,
        })
      }
      closeModal()
      refetch()
    } catch (e) {
      setModalError(e instanceof ApiError ? (e.problem?.detail ?? e.message) : 'Failed to save user')
    } finally {
      setModalSaving(false)
    }
  }

  const handleToggleActive = async (user: UserResponse) => {
    try {
      if (user.active) {
        await usersApi.deactivate(user.id)
      } else {
        await usersApi.activate(user.id)
      }
      refetch()
    } catch (e) {
      setFetchState({ status: 'error', message: e instanceof ApiError ? e.message : 'Failed to update user status' })
    }
  }

  const users = data?.content ?? []
  const totalElements = data?.total_elements ?? 0
  const totalPages = data?.total_pages ?? 0
  const showFrom = totalElements > 0 ? page * 25 + 1 : 0
  const showTo = Math.min((page + 1) * 25, totalElements)

  return (
    <div className="users-page">
      <div className="page-header">
        <h2>Users & Roles</h2>
        <button type="button" className="btn-add" onClick={openCreateModal}>
          <svg viewBox="0 0 16 16" fill="none">
            <path d="M8 3v10M3 8h10" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
          </svg>
          Add user
        </button>
      </div>

      <div className="tabs">
        <button type="button" className="tab-btn active">
          Users <span className="tab-count">{totalElements}</span>
        </button>
        <Link to="/settings/roles" className="tab-btn">
          Roles
        </Link>
      </div>

      {/* Toolbar */}
      <div className="toolbar">
        <div className="search-box">
          <svg viewBox="0 0 16 16" fill="none">
            <circle cx="7" cy="7" r="5" stroke="currentColor" strokeWidth="1.5"/>
            <path d="M11 11l3.5 3.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
          </svg>
          <input
            type="text"
            placeholder="Search users by name or email…"
            value={search}
            onChange={(e) => handleSearch(e.target.value)}
          />
        </div>
        <div className="filter-group">
          {ROLE_FILTERS.map((f) => (
            <button
              key={f.value}
              type="button"
              className={`filter-btn${roleFilter === f.value ? ' active' : ''}`}
              onClick={() => handleRoleFilter(f.value)}
            >
              {f.label}
            </button>
          ))}
        </div>
        <div className="filter-group">
          {STATUS_FILTERS.map((f) => (
            <button
              key={String(f.value)}
              type="button"
              className={`filter-btn${statusFilter === f.value ? ' active' : ''}`}
              onClick={() => handleStatusFilter(f.value)}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>

      {/* Error */}
      {error && (
        <div className="mb-4 rounded-md bg-red-50 p-3 text-sm text-red-700">{error}</div>
      )}

      {/* Data table */}
      <div className="data-card">
        <table>
          <thead>
            <tr>
              <th>User</th>
              <th>Role</th>
              <th>Status</th>
              <th>Last login</th>
              <th>Created</th>
              <th style={{ width: 80 }}></th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <tr key={i} className="skeleton-row">
                  <td><div className="skeleton-bar" style={{ width: 180 }} /></td>
                  <td><div className="skeleton-bar" style={{ width: 80 }} /></td>
                  <td><div className="skeleton-bar" style={{ width: 60 }} /></td>
                  <td><div className="skeleton-bar" style={{ width: 80 }} /></td>
                  <td><div className="skeleton-bar" style={{ width: 80 }} /></td>
                  <td />
                </tr>
              ))
            ) : users.length === 0 ? (
              <tr>
                <td colSpan={6}>
                  <div className="empty-state">
                    <p>No users found</p>
                    <p className="sub">
                      {search || roleFilter || statusFilter !== undefined
                        ? 'Try adjusting your filters'
                        : 'Add users to get started'}
                    </p>
                  </div>
                </td>
              </tr>
            ) : (
              users.map((user) => (
                <tr key={user.id}>
                  <td>
                    <div className="user-cell">
                      <div className={`user-avatar avatar-${user.role}`}>
                        {userInitials(user.full_name, user.username)}
                      </div>
                      <div>
                        <div className="user-name">{user.full_name}</div>
                        <div className="user-email">{user.email}</div>
                      </div>
                    </div>
                  </td>
                  <td>
                    <RoleBadge role={user.role} displayName={user.role_display_name} />
                  </td>
                  <td>
                    <StatusBadge active={user.active} />
                  </td>
                  <td className="meta-text">{formatDate(user.last_login_at)}</td>
                  <td className="meta-text">{formatCreatedDate(user.created_at)}</td>
                  <td>
                    <div className="row-actions">
                      <button
                        type="button"
                        className="action-btn"
                        title="Edit"
                        onClick={() => openEditModal(user)}
                      >
                        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                          <path d="M11.5 2.5l2 2L5 13H3v-2l8.5-8.5z" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" strokeLinejoin="round"/>
                        </svg>
                      </button>
                      <button
                        type="button"
                        className="action-btn"
                        title={user.active ? 'Deactivate' : 'Activate'}
                        onClick={() => handleToggleActive(user)}
                      >
                        {user.active ? (
                          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                            <circle cx="8" cy="8" r="6" stroke="currentColor" strokeWidth="1.3"/>
                            <path d="M5.5 5.5l5 5M10.5 5.5l-5 5" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round"/>
                          </svg>
                        ) : (
                          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                            <circle cx="8" cy="8" r="6" stroke="currentColor" strokeWidth="1.3"/>
                            <path d="M5.5 8l2 2 3-4" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" strokeLinejoin="round"/>
                          </svg>
                        )}
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {!loading && totalElements > 0 && (
          <div className="pagination">
            <span>Showing {showFrom}–{showTo} of {totalElements} users</span>
            <div className="page-btns">
              {Array.from({ length: totalPages }).map((_, i) => (
                <button
                  key={i}
                  type="button"
                  className={`page-btn${page === i ? ' active' : ''}`}
                  onClick={() => setPage(i)}
                >
                  {i + 1}
                </button>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Create/Edit Modal */}
      {modalUser !== undefined && (
        <UserDetailModal
          user={modalUser}
          onClose={closeModal}
          onSave={handleSave}
          saving={modalSaving}
          error={modalError}
        />
      )}
    </div>
  )
}

import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { ApiError } from '@/api/client'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { PageHeader } from '@/components/layout/PageHeader'
import { LoadingRows } from '@/components/feedback/LoadingRows'
import { usersApi } from '../api/usersApi'
import type { UserListParams } from '../api/usersApi'
import { RoleBadge } from '../components/RoleBadge'
import { StatusBadge } from '../components/StatusBadge'
import { UserDetailModal } from '../components/UserDetailModal'
import type { UserResponse, UsersPageResponse } from '../types/user'
import { Pencil, Plus, Search, ShieldCheck, ShieldX, Users } from 'lucide-react'

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

const AVATAR_COLORS: Record<string, string> = {
  PS_ANALYST: 'bg-accent-sky/15 text-accent-sky',
  PS_SALES_LEAD: 'bg-accent-violet/15 text-accent-violet',
  PS_ADMIN: 'bg-accent-amber/15 text-accent-amber',
  PS_VIEWER: 'bg-accent-emerald/15 text-accent-emerald',
  PS_COO: 'bg-accent-rose/15 text-accent-rose',
  PS_TELECALLER: 'bg-accent-teal/15 text-accent-teal',
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

  const handleSearch = (value: string) => { setSearch(value); setPage(0) }
  const handleRoleFilter = (value: string) => { setRoleFilter(value); setPage(0) }
  const handleStatusFilter = (value: boolean | undefined) => { setStatusFilter(value); setPage(0) }

  const openCreateModal = () => { setModalUser(null); setModalError(null) }
  const openEditModal = (user: UserResponse) => { setModalUser(user); setModalError(null) }
  const closeModal = () => { setModalUser(undefined); setModalError(null) }

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
    <div className="space-y-6">
      <PageHeader
        title="Users & Roles"
        description="Manage user accounts and their role assignments."
        actions={
          <Button onClick={openCreateModal}><Plus /> Add user</Button>
        }
      />

      <div className="flex gap-1 border-b border-border">
        <button
          type="button"
          className="inline-flex items-center gap-2 border-b-2 border-primary px-4 py-2.5 text-sm font-semibold text-primary"
        >
          <Users className="size-4" />
          Users
          <span className="inline-flex items-center justify-center rounded-full bg-primary/10 px-2 py-0.5 text-[11px] font-bold tabular-nums">
            {totalElements}
          </span>
        </button>
        <Link
          to="/settings/roles"
          className="inline-flex items-center gap-2 border-b-2 border-transparent px-4 py-2.5 text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
        >
          Roles
        </Link>
      </div>

      <div className="flex flex-wrap items-end gap-3 rounded-xl border border-border bg-card p-4 shadow-card">
        <div className="relative min-w-[260px] flex-1">
          <Search className="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground/50" />
          <Input
            placeholder="Search users by name or email..."
            className="pl-8"
            value={search}
            onChange={(e) => handleSearch(e.target.value)}
          />
        </div>
        <div className="flex gap-0.5 rounded-lg border border-border bg-surface-1 p-0.5">
          {ROLE_FILTERS.map((f) => (
            <button
              key={f.value}
              type="button"
              className={`rounded-md px-3 py-1.5 text-xs font-semibold transition-all duration-200 ${
                roleFilter === f.value
                  ? 'bg-card text-foreground shadow-sm'
                  : 'text-muted-foreground hover:text-foreground'
              }`}
              onClick={() => handleRoleFilter(f.value)}
            >
              {f.label}
            </button>
          ))}
        </div>
        <div className="flex gap-0.5 rounded-lg border border-border bg-surface-1 p-0.5">
          {STATUS_FILTERS.map((f) => (
            <button
              key={String(f.value)}
              type="button"
              className={`rounded-md px-3 py-1.5 text-xs font-semibold transition-all duration-200 ${
                statusFilter === f.value
                  ? 'bg-card text-foreground shadow-sm'
                  : 'text-muted-foreground hover:text-foreground'
              }`}
              onClick={() => handleStatusFilter(f.value)}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>

      {error && (
        <div className="rounded-xl border border-destructive/30 bg-destructive/5 p-4 text-sm font-medium text-destructive">{error}</div>
      )}

      <div className="overflow-hidden rounded-xl border border-border bg-card shadow-card">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border bg-surface-1">
                <th className="py-3 px-4 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">User</th>
                <th className="py-3 px-4 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Role</th>
                <th className="py-3 px-4 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Status</th>
                <th className="py-3 px-4 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Last login</th>
                <th className="py-3 px-4 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Created</th>
                <th className="py-3 px-4 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground" style={{ width: 80 }}></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {loading ? (
                <tr><td colSpan={6} className="p-4"><LoadingRows count={5} /></td></tr>
              ) : users.length === 0 ? (
                <tr>
                  <td colSpan={6}>
                    <div className="py-16 text-center">
                      <p className="font-semibold">No users found</p>
                      <p className="text-sm text-muted-foreground mt-1">
                        {search || roleFilter || statusFilter !== undefined
                          ? 'Try adjusting your filters'
                          : 'Add users to get started'}
                      </p>
                    </div>
                  </td>
                </tr>
              ) : (
                users.map((user) => (
                  <tr key={user.id} className="hover:bg-accent/30 transition-colors duration-150 group/row">
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <div className={`flex size-9 items-center justify-center rounded-xl text-xs font-bold ${AVATAR_COLORS[user.role] ?? 'bg-surface-1 text-muted-foreground'}`}>
                          {userInitials(user.full_name, user.username)}
                        </div>
                        <div className="min-w-0">
                          <div className="font-semibold truncate">{user.full_name}</div>
                          <div className="text-xs text-muted-foreground truncate">{user.email}</div>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <RoleBadge role={user.role} displayName={user.role_display_name} />
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge active={user.active} />
                    </td>
                    <td className="px-4 py-3 text-muted-foreground text-[13px]">{formatDate(user.last_login_at)}</td>
                    <td className="px-4 py-3 text-muted-foreground text-[13px]">{formatCreatedDate(user.created_at)}</td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-1 opacity-60 group-hover/row:opacity-100 transition-opacity">
                        <Button
                          variant="ghost"
                          size="icon-xs"
                          onClick={() => openEditModal(user)}
                          aria-label="Edit user"
                        >
                          <Pencil className="size-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon-xs"
                          onClick={() => handleToggleActive(user)}
                          aria-label={user.active ? 'Deactivate' : 'Activate'}
                          className={user.active ? 'text-muted-foreground hover:text-accent-rose' : 'text-muted-foreground hover:text-accent-emerald'}
                        >
                          {user.active ? <ShieldX className="size-4" /> : <ShieldCheck className="size-4" />}
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {!loading && totalElements > 0 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-border bg-surface-1/50 text-sm">
            <span className="text-muted-foreground">
              Showing <span className="font-semibold text-foreground tabular-nums">{showFrom}–{showTo}</span> of <span className="font-semibold text-foreground tabular-nums">{totalElements}</span> users
            </span>
            <div className="flex gap-1">
              {Array.from({ length: totalPages }).map((_, i) => (
                <button
                  key={i}
                  type="button"
                  className={`flex size-8 items-center justify-center rounded-lg text-xs font-semibold transition-all duration-200 ${
                    page === i
                      ? 'bg-primary text-primary-foreground shadow-sm'
                      : 'text-muted-foreground hover:bg-accent hover:text-foreground'
                  }`}
                  onClick={() => setPage(i)}
                >
                  {i + 1}
                </button>
              ))}
            </div>
          </div>
        )}
      </div>

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

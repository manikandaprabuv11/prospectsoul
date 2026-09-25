import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { ApiError } from '@/api/client'
import { PageHeader } from '@/components/layout/PageHeader'
import { LoadingRows } from '@/components/feedback/LoadingRows'
import { Shield, Users } from 'lucide-react'
import { rolesApi } from '../api/rolesApi'
import { PermissionTag } from '../components/PermissionTag'
import { RoleDetailDrawer } from '../components/RoleDetailDrawer'
import type { RoleResponse } from '../types/role'

const ROLE_ACCENT: Record<string, string> = {
  PS_ANALYST: 'accent-sky',
  PS_SALES_LEAD: 'accent-violet',
  PS_ADMIN: 'accent-amber',
  PS_VIEWER: 'accent-emerald',
  PS_COO: 'accent-rose',
  PS_TELECALLER: 'accent-teal',
}

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
    <div className="space-y-6">
      <PageHeader
        title="Users & Roles"
        description="View role definitions and their permission sets."
      />

      <div className="flex gap-1 border-b border-border">
        <Link
          to="/settings/users"
          className="inline-flex items-center gap-2 border-b-2 border-transparent px-4 py-2.5 text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
        >
          <Users className="size-4" />
          Users
        </Link>
        <button
          type="button"
          className="inline-flex items-center gap-2 border-b-2 border-primary px-4 py-2.5 text-sm font-semibold text-primary"
        >
          <Shield className="size-4" />
          Roles
          <span className="inline-flex items-center justify-center rounded-full bg-primary/10 px-2 py-0.5 text-[11px] font-bold tabular-nums">
            {roles.length}
          </span>
        </button>
      </div>

      {error && (
        <div className="rounded-xl border border-destructive/30 bg-destructive/5 p-4 text-sm font-medium text-destructive">{error}</div>
      )}

      {loading ? (
        <LoadingRows count={5} height="h-32" />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {roles.map((role) => {
            const accent = ROLE_ACCENT[role.name] ?? 'accent-indigo'
            return (
              <button
                key={role.id}
                type="button"
                className={`accent-stripe ${accent} group relative flex flex-col gap-4 rounded-xl border border-border bg-card p-5 text-left shadow-card transition-all duration-200 hover:shadow-card-hover hover:-translate-y-0.5 cursor-pointer`}
                onClick={() => setSelectedRoleId(role.id)}
              >
                <div className="flex items-start justify-between">
                  <div className={`accent-chip-bg ${accent} flex size-10 items-center justify-center rounded-xl transition-transform duration-200 group-hover:scale-110`}>
                    <Shield className="size-5" />
                  </div>
                  <span className="inline-flex items-center gap-1.5 text-xs font-medium text-muted-foreground">
                    <Users className="size-3.5" />
                    {role.user_count} user{role.user_count !== 1 ? 's' : ''}
                  </span>
                </div>
                <div>
                  <h3 className="text-[15px] font-bold tracking-tight">{role.display_name}</h3>
                  <p className="text-[13px] text-muted-foreground leading-relaxed mt-1">
                    {role.description || 'No description'}
                  </p>
                </div>
                <div className="flex flex-wrap gap-1">
                  {role.permissions.map((p) => (
                    <PermissionTag key={p} permission={p} />
                  ))}
                </div>
              </button>
            )
          })}
        </div>
      )}

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

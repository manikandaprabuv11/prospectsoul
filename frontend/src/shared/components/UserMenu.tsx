import { useAuth } from '@/auth/useAuth'
import { ChevronDown, LogOut } from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'

const ROLE_LABELS: Record<string, string> = {
  PS_ADMIN: 'Admin',
  PS_ANALYST: 'Analyst',
  PS_SALES_LEAD: 'Sales Lead',
  PS_VIEWER: 'Viewer',
  PS_COO: 'COO',
}

function primaryRoleLabel(roles: string[]): string {
  const priority = ['PS_ADMIN', 'PS_COO', 'PS_SALES_LEAD', 'PS_ANALYST', 'PS_VIEWER']
  for (const r of priority) {
    if (roles.includes(r)) return ROLE_LABELS[r] ?? r
  }
  return 'User'
}

function initials(fullName: string, username: string): string {
  if (fullName.trim()) {
    return fullName
      .split(/\s+/)
      .slice(0, 2)
      .map((w) => w[0]?.toUpperCase() ?? '')
      .join('')
  }
  return (username[0] ?? 'U').toUpperCase()
}

export function UserMenu() {
  const { user, roles, logout } = useAuth()
  const [open, setOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  const handleClickOutside = useCallback((e: MouseEvent) => {
    if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
      setOpen(false)
    }
  }, [])

  useEffect(() => {
    if (open) {
      document.addEventListener('mousedown', handleClickOutside)
      return () => document.removeEventListener('mousedown', handleClickOutside)
    }
  }, [open, handleClickOutside])

  if (!user) return null

  const label = primaryRoleLabel(roles)
  const avatar = initials(user.fullName, user.username)

  return (
    <div ref={menuRef} className="relative">
      <button
        type="button"
        onClick={() => setOpen(!open)}
        className="flex items-center gap-2 rounded-md px-2 py-1.5 text-sm transition-colors hover:bg-accent"
      >
        <div className="flex size-8 items-center justify-center rounded-full bg-primary text-xs font-medium text-primary-foreground">
          {avatar}
        </div>
        <div className="hidden text-left md:block">
          <div className="text-sm font-medium leading-tight">{user.fullName || user.username}</div>
          <div className="text-xs text-muted-foreground">{label}</div>
        </div>
        <ChevronDown className="hidden size-3.5 text-muted-foreground md:block" />
      </button>

      {open && (
        <div className="absolute right-0 top-full z-50 mt-1 w-56 rounded-md border bg-popover p-1 shadow-md">
          <div className="border-b px-3 py-2">
            <div className="text-sm font-medium">{user.fullName || user.username}</div>
            <div className="text-xs text-muted-foreground">{user.email}</div>
            <span className="mt-1 inline-block rounded-full bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary">
              {label}
            </span>
          </div>
          <button
            type="button"
            onClick={() => {
              setOpen(false)
              logout()
            }}
            className="mt-1 flex w-full items-center gap-2 rounded-sm px-3 py-2 text-sm text-destructive transition-colors hover:bg-destructive/10"
          >
            <LogOut className="size-4" />
            Sign out
          </button>
        </div>
      )}
    </div>
  )
}

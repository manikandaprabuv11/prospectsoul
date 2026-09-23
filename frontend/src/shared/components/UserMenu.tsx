import { useAuth } from '@/auth/useAuth'
import { ChevronDown, LogOut, Mail, User } from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'

const ROLE_LABELS: Record<string, string> = {
  PS_ADMIN: 'Admin',
  PS_ANALYST: 'Analyst',
  PS_SALES_LEAD: 'Sales Lead',
  PS_VIEWER: 'Viewer',
  PS_COO: 'COO',
  PS_TELECALLER: 'Telecaller',
}

function primaryRoleLabel(roles: string[]): string {
  const priority = ['PS_ADMIN', 'PS_COO', 'PS_SALES_LEAD', 'PS_ANALYST', 'PS_VIEWER', 'PS_TELECALLER']
  for (const r of priority) if (roles.includes(r)) return ROLE_LABELS[r] ?? r
  return 'User'
}

function initials(fullName: string, username: string): string {
  const source = (fullName || username || '').trim()
  if (!source) return 'U'
  return source.split(/\s+/).slice(0, 2).map((w) => w[0]?.toUpperCase() ?? '').join('') || 'U'
}

/** Elegant, dropdown-driven identity chip in the top-right of the shell. */
export function UserMenu() {
  const { user, roles, logout } = useAuth()
  const [open, setOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  const handleClickOutside = useCallback((e: MouseEvent) => {
    if (menuRef.current && !menuRef.current.contains(e.target as Node)) setOpen(false)
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
        aria-haspopup="menu"
        aria-expanded={open}
        className="flex items-center gap-2 rounded-md p-1 pr-2 text-sm transition-colors hover:bg-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/50"
      >
        <div className="flex size-8 items-center justify-center rounded-full bg-primary text-xs font-semibold text-primary-foreground shadow-sm">
          {avatar}
        </div>
        <div className="hidden text-left md:block leading-tight">
          <div className="text-sm font-medium">{user.fullName || user.username}</div>
          <div className="text-xs text-muted-foreground">{label}</div>
        </div>
        <ChevronDown className="hidden size-3.5 text-muted-foreground md:block" aria-hidden="true" />
      </button>

      {open && (
        <div
          role="menu"
          className="absolute right-0 top-full z-50 mt-1.5 w-64 overflow-hidden rounded-lg border border-border/70 bg-popover shadow-lg animate-in fade-in-0 zoom-in-95 slide-in-from-top-1"
        >
          <div className="p-4 border-b border-border/70 bg-muted/40">
            <div className="flex items-center gap-3">
              <div className="flex size-10 items-center justify-center rounded-full bg-primary text-sm font-semibold text-primary-foreground shrink-0">
                {avatar}
              </div>
              <div className="min-w-0">
                <div className="text-sm font-semibold truncate">{user.fullName || user.username}</div>
                <div className="inline-flex items-center rounded-full bg-primary/10 px-2 py-0.5 mt-0.5 text-[10px] font-medium text-primary">
                  {label}
                </div>
              </div>
            </div>
            <div className="mt-3 space-y-1 text-xs text-muted-foreground">
              <div className="flex items-center gap-1.5"><User className="size-3" aria-hidden="true" /> {user.username}</div>
              <div className="flex items-center gap-1.5 min-w-0"><Mail className="size-3 shrink-0" aria-hidden="true" /> <span className="truncate">{user.email}</span></div>
            </div>
          </div>
          <div className="p-1">
            <button
              type="button"
              role="menuitem"
              onClick={() => { setOpen(false); logout() }}
              className="flex w-full items-center gap-2 rounded-md px-3 py-2 text-sm text-destructive transition-colors hover:bg-destructive/10"
            >
              <LogOut className="size-4" aria-hidden="true" />
              Sign out
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

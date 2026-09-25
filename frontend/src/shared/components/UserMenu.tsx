import { useAuth } from '@/auth/useAuth'
import { useTheme } from '@/hooks/useTheme'
import { cn } from '@/lib/utils'
import { Check, ChevronDown, LogOut, Mail, Monitor, Moon, Sun, User } from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'
import type { Theme } from '@/hooks/useTheme'

const ROLE_LABELS: Record<string, string> = {
  PS_ADMIN: 'Admin',
  PS_ANALYST: 'Analyst',
  PS_SALES_LEAD: 'Sales Lead',
  PS_VIEWER: 'Viewer',
  PS_COO: 'COO',
  PS_TELECALLER: 'Telecaller',
}

const ROLE_COLORS: Record<string, string> = {
  PS_ADMIN: 'bg-accent-rose/15 text-accent-rose',
  PS_ANALYST: 'bg-accent-teal/15 text-accent-teal',
  PS_SALES_LEAD: 'bg-accent-violet/15 text-accent-violet',
  PS_VIEWER: 'bg-accent-sky/15 text-accent-sky',
  PS_COO: 'bg-accent-amber/15 text-accent-amber',
  PS_TELECALLER: 'bg-accent-emerald/15 text-accent-emerald',
}

function primaryRoleLabel(roles: string[]): string {
  const priority = ['PS_ADMIN', 'PS_COO', 'PS_SALES_LEAD', 'PS_ANALYST', 'PS_VIEWER', 'PS_TELECALLER']
  for (const r of priority) if (roles.includes(r)) return ROLE_LABELS[r] ?? r
  return 'User'
}

function primaryRoleColor(roles: string[]): string {
  const priority = ['PS_ADMIN', 'PS_COO', 'PS_SALES_LEAD', 'PS_ANALYST', 'PS_VIEWER', 'PS_TELECALLER']
  for (const r of priority) if (roles.includes(r)) return ROLE_COLORS[r] ?? 'bg-muted text-muted-foreground'
  return 'bg-muted text-muted-foreground'
}

function initials(fullName: string, username: string): string {
  const source = (fullName || username || '').trim()
  if (!source) return 'U'
  return source.split(/\s+/).slice(0, 2).map((w) => w[0]?.toUpperCase() ?? '').join('') || 'U'
}

const THEME_OPTIONS: { value: Theme; label: string; icon: React.ComponentType<{ className?: string }> }[] = [
  { value: 'light', label: 'Light', icon: Sun },
  { value: 'dark', label: 'Dark', icon: Moon },
  { value: 'system', label: 'System', icon: Monitor },
]

export function UserMenu() {
  const { user, roles, logout } = useAuth()
  const { theme, setTheme } = useTheme()
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
  const roleColor = primaryRoleColor(roles)
  const avatar = initials(user.fullName, user.username)

  return (
    <div ref={menuRef} className="relative">
      <button
        type="button"
        onClick={() => setOpen(!open)}
        aria-haspopup="menu"
        aria-expanded={open}
        className="flex items-center gap-2 rounded-lg p-1 pr-2 text-sm transition-all duration-200 hover:bg-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/50"
      >
        <div className="flex size-8 items-center justify-center rounded-lg bg-brand-gradient text-[11px] font-bold text-white shadow-sm">
          {avatar}
        </div>
        <div className="hidden text-left md:block leading-tight">
          <div className="text-[13px] font-semibold text-foreground">{user.fullName || user.username}</div>
          <div className="text-[11px] text-muted-foreground">{label}</div>
        </div>
        <ChevronDown className={cn(
          'hidden size-3.5 text-muted-foreground md:block transition-transform duration-200',
          open && 'rotate-180',
        )} aria-hidden="true" />
      </button>

      {open && (
        <div
          role="menu"
          className="absolute right-0 top-full z-50 mt-2 w-72 overflow-hidden rounded-xl border border-border bg-popover shadow-xl animate-in fade-in-0 zoom-in-95 slide-in-from-top-2 duration-200"
        >
          {/* User info */}
          <div className="p-4 bg-brand-gradient-subtle border-b border-border">
            <div className="flex items-center gap-3">
              <div className="flex size-11 items-center justify-center rounded-xl bg-brand-gradient text-sm font-bold text-white shadow-primary shrink-0">
                {avatar}
              </div>
              <div className="min-w-0">
                <div className="text-sm font-semibold truncate">{user.fullName || user.username}</div>
                <div className={cn(
                  'inline-flex items-center rounded-md px-2 py-0.5 mt-1 text-[10px] font-semibold',
                  roleColor,
                )}>
                  {label}
                </div>
              </div>
            </div>
            <div className="mt-3 space-y-1.5 text-xs text-muted-foreground">
              <div className="flex items-center gap-2"><User className="size-3 shrink-0" aria-hidden="true" /> {user.username}</div>
              <div className="flex items-center gap-2 min-w-0"><Mail className="size-3 shrink-0" aria-hidden="true" /> <span className="truncate">{user.email}</span></div>
            </div>
          </div>

          {/* Theme switcher */}
          <div className="p-2 border-b border-border">
            <div className="px-2 py-1.5 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
              Appearance
            </div>
            <div className="flex gap-1 p-1 bg-muted/50 rounded-lg">
              {THEME_OPTIONS.map((opt) => (
                <button
                  key={opt.value}
                  type="button"
                  onClick={() => setTheme(opt.value)}
                  className={cn(
                    'flex flex-1 items-center justify-center gap-1.5 rounded-md px-2 py-1.5 text-xs font-medium transition-all duration-200',
                    theme === opt.value
                      ? 'bg-background text-foreground shadow-sm'
                      : 'text-muted-foreground hover:text-foreground',
                  )}
                >
                  <opt.icon className="size-3.5" />
                  {opt.label}
                  {theme === opt.value && <Check className="size-3 text-primary" />}
                </button>
              ))}
            </div>
          </div>

          {/* Actions */}
          <div className="p-1.5">
            <button
              type="button"
              role="menuitem"
              onClick={() => { setOpen(false); logout() }}
              className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-sm text-destructive transition-all duration-200 hover:bg-destructive/8"
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

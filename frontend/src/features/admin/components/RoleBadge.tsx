import { cn } from '@/lib/utils'

const ROLE_STYLES: Record<string, string> = {
  PS_ANALYST: 'bg-accent-sky/10 text-accent-sky border border-accent-sky/20',
  PS_SALES_LEAD: 'bg-accent-violet/10 text-accent-violet border border-accent-violet/20',
  PS_ADMIN: 'bg-accent-amber/10 text-accent-amber border border-accent-amber/20',
  PS_VIEWER: 'bg-accent-emerald/10 text-accent-emerald border border-accent-emerald/20',
  PS_COO: 'bg-accent-rose/10 text-accent-rose border border-accent-rose/20',
  PS_TELECALLER: 'bg-accent-teal/10 text-accent-teal border border-accent-teal/20',
}

const ROLE_LABELS: Record<string, string> = {
  PS_ANALYST: 'Analyst',
  PS_SALES_LEAD: 'Sales Lead',
  PS_ADMIN: 'Admin',
  PS_VIEWER: 'Viewer',
  PS_COO: 'COO',
  PS_TELECALLER: 'Telecaller',
}

export function RoleBadge({ role, displayName }: { role: string; displayName?: string }) {
  const style = ROLE_STYLES[role] ?? 'bg-surface-1 text-muted-foreground border border-border'
  const label = displayName ?? ROLE_LABELS[role] ?? role

  return (
    <span className={cn('inline-flex items-center rounded-md px-2.5 py-0.5 text-[11px] font-semibold', style)}>
      {label}
    </span>
  )
}

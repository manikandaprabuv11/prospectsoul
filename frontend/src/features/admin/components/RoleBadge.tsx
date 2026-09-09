import { cn } from '@/lib/utils'

const ROLE_STYLES: Record<string, string> = {
  PS_ANALYST: 'bg-[#e8f0fc] text-[#2d5fa3]',
  PS_SALES_LEAD: 'bg-[#ede8fc] text-[#5a44a3]',
  PS_ADMIN: 'bg-[#f8ead5] text-[#96592b]',
  PS_VIEWER: 'bg-[#e8f5ee] text-[#1e6b44]',
  PS_COO: 'bg-[#fce8ed] text-[#963b4a]',
}

const ROLE_LABELS: Record<string, string> = {
  PS_ANALYST: 'Analyst',
  PS_SALES_LEAD: 'Sales Lead',
  PS_ADMIN: 'Admin',
  PS_VIEWER: 'Viewer',
  PS_COO: 'COO',
}

export function RoleBadge({ role, displayName }: { role: string; displayName?: string }) {
  const style = ROLE_STYLES[role] ?? 'bg-gray-100 text-gray-600'
  const label = displayName ?? ROLE_LABELS[role] ?? role

  return (
    <span className={cn('inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold', style)}>
      {label}
    </span>
  )
}

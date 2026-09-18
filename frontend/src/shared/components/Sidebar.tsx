import { usePermissions } from '@/auth/usePermissions'
import { cn } from '@/lib/utils'
import { Building2, FileUp, LayoutDashboard, ShieldCheck, Users } from 'lucide-react'
import { useMemo } from 'react'
import { Link, useLocation } from 'react-router'

interface NavItem {
  to: string
  label: string
  icon: React.ComponentType<{ className?: string }>
  visible: (p: { canRead: boolean; canMutate: boolean; canExport: boolean; canConfigure: boolean }) => boolean
}

const navItems: NavItem[] = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, visible: () => true },
  { to: '/companies', label: 'Companies', icon: Building2, visible: ({ canRead }) => canRead },
  { to: '/imports', label: 'Imports', icon: FileUp, visible: ({ canMutate }) => canMutate },
  { to: '/verify', label: 'Verify', icon: ShieldCheck, visible: ({ canRead }) => canRead },
  { to: '/settings/users', label: 'Users & Roles', icon: Users, visible: ({ canConfigure }) => canConfigure },
]

export function Sidebar() {
  const location = useLocation()
  const permissions = usePermissions()

  const visibleItems = useMemo(
    () => navItems.filter((item) => item.visible(permissions)),
    [permissions],
  )

  return (
    <aside className="flex w-56 shrink-0 flex-col border-r bg-card">
      <nav className="flex flex-1 flex-col gap-1 p-3">
        {visibleItems.map((item) => {
          const active =
            item.to === '/'
              ? location.pathname === '/'
              : location.pathname.startsWith(item.to)

          return (
            <Link
              key={item.to}
              to={item.to}
              className={cn(
                'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
                active
                  ? 'bg-primary/10 text-primary'
                  : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground',
              )}
            >
              <item.icon className="size-4" />
              {item.label}
            </Link>
          )
        })}
      </nav>
    </aside>
  )
}

import { usePermissions } from '@/auth/usePermissions'
import { cn } from '@/lib/utils'
import {
  Building2, FileUp, Filter, Layers, LayoutDashboard, ListTree, MapPin,
  Settings2, ShieldCheck, UserSquare2, Users, Sparkles, X,
} from 'lucide-react'
import { useMemo } from 'react'
import { Link, useLocation } from 'react-router'

type PermFlags = { canRead: boolean; canMutate: boolean; canExport: boolean; canConfigure: boolean }

interface NavItem {
  to: string
  label: string
  icon: React.ComponentType<{ className?: string }>
  visible: (p: PermFlags) => boolean
  exact?: boolean
}

interface NavSection {
  label: string
  items: NavItem[]
}

const sections: NavSection[] = [
  {
    label: 'Workspace',
    items: [
      { to: '/',             label: 'Dashboard', icon: LayoutDashboard, visible: () => true, exact: true },
      { to: '/companies',    label: 'Companies', icon: Building2,       visible: ({ canRead }) => canRead },
      { to: '/companies/map',label: 'Map',        icon: MapPin,          visible: ({ canRead }) => canRead },
    ],
  },
  {
    label: 'Data',
    items: [
      { to: '/imports', label: 'Imports', icon: FileUp,      visible: ({ canMutate }) => canMutate },
      { to: '/verify',  label: 'Verify',  icon: ShieldCheck, visible: ({ canRead }) => canRead },
      { to: '/enrichment/jobs', label: 'Enrichment', icon: Layers, visible: ({ canRead }) => canRead },
    ],
  },
  {
    label: 'Settings',
    items: [
      { to: '/settings/users',            label: 'Users & Roles',      icon: Users,        visible: ({ canConfigure }) => canConfigure },
      { to: '/settings/company-defaults', label: 'Company defaults',   icon: Filter,       visible: ({ canConfigure }) => canConfigure },
      { to: '/settings/nic-codes',     label: 'NIC Codes',      icon: ListTree,     visible: ({ canConfigure }) => canConfigure },
      { to: '/settings/contact-roles', label: 'Contact Roles',  icon: UserSquare2,  visible: ({ canConfigure }) => canConfigure },
      { to: '/settings/enrichment-providers', label: 'Enrichment Providers', icon: Settings2, visible: ({ canConfigure }) => canConfigure },
    ],
  },
]

interface Props {
  mobileOpen: boolean
  onMobileClose: () => void
}

export function AppSidebar({ mobileOpen, onMobileClose }: Props) {
  const location = useLocation()
  const permissions = usePermissions()

  const visibleSections = useMemo(
    () =>
      sections
        .map((s) => ({ ...s, items: s.items.filter((i) => i.visible(permissions)) }))
        .filter((s) => s.items.length > 0),
    [permissions],
  )

  const isActive = (item: NavItem) =>
    item.exact ? location.pathname === item.to : location.pathname.startsWith(item.to)

  return (
    <>
      {/* Mobile slide-over overlay */}
      <div
        className={cn(
          'fixed inset-0 z-40 bg-foreground/40 backdrop-blur-sm transition-opacity lg:hidden',
          mobileOpen ? 'opacity-100' : 'pointer-events-none opacity-0',
        )}
        aria-hidden="true"
        onClick={onMobileClose}
      />
      <aside
        className={cn(
          'fixed z-50 flex h-svh w-64 shrink-0 flex-col border-r border-border/70 bg-card transition-transform lg:sticky lg:top-0 lg:translate-x-0',
          mobileOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0',
        )}
        aria-label="Primary navigation"
      >
        {/* Brand */}
        <div className="flex h-14 shrink-0 items-center justify-between border-b border-border/70 px-4">
          <Link to="/" className="flex items-center gap-2 group" onClick={onMobileClose}>
            <span className="flex size-9 items-center justify-center rounded-lg bg-brand-gradient text-white shadow-primary">
              <Sparkles className="size-4.5" />
            </span>
            <div className="leading-tight">
              <div className="text-sm font-semibold tracking-tight text-brand-gradient">ProspectSoul</div>
              <div className="text-[10px] uppercase tracking-wider text-muted-foreground">Enterprise sales intelligence</div>
            </div>
          </Link>
          <button
            type="button"
            className="lg:hidden inline-flex size-8 items-center justify-center rounded-md text-muted-foreground hover:bg-accent hover:text-accent-foreground"
            onClick={onMobileClose}
            aria-label="Close navigation"
          >
            <X className="size-4" />
          </button>
        </div>

        {/* Sections */}
        <nav className="flex-1 overflow-y-auto px-3 py-4">
          {visibleSections.map((section) => (
            <div key={section.label} className="mb-5 last:mb-0">
              <div className="mb-1.5 px-3 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground/80">
                {section.label}
              </div>
              <div className="flex flex-col gap-0.5">
                {section.items.map((item) => {
                  const active = isActive(item)
                  return (
                    <Link
                      key={item.to}
                      to={item.to}
                      onClick={onMobileClose}
                      className={cn(
                        'group relative flex items-center gap-2.5 rounded-md px-3 py-2 text-sm font-medium transition-colors',
                        active
                          ? 'bg-primary/10 text-primary'
                          : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground',
                      )}
                    >
                      {active ? (
                        <span
                          aria-hidden="true"
                          className="absolute left-0 top-1.5 bottom-1.5 w-0.5 rounded-r-full bg-primary"
                        />
                      ) : null}
                      <item.icon className={cn('size-4 shrink-0', active ? 'text-primary' : 'text-muted-foreground group-hover:text-accent-foreground')} />
                      <span className="truncate">{item.label}</span>
                    </Link>
                  )
                })}
              </div>
            </div>
          ))}
        </nav>

        {/* Footer badge */}
        <div className="border-t border-border/70 p-3">
          <div className="relative overflow-hidden rounded-lg p-3 text-xs text-white shadow-primary bg-brand-gradient">
            <div className="absolute -right-4 -bottom-4 size-16 rounded-full bg-white/10 blur-xl" aria-hidden="true" />
            <div className="relative font-semibold">ProspectSoul v1.0</div>
            <div className="relative text-white/80 mt-0.5">Vyoog Information Pvt Ltd</div>
          </div>
        </div>
      </aside>
    </>
  )
}

import { usePermissions } from '@/auth/usePermissions'
import { useTheme } from '@/hooks/useTheme'
import { cn } from '@/lib/utils'
import {
  Building2, ChevronLeft, FileUp, Filter, Layers, LayoutDashboard, ListTree, MapPin,
  Moon, Settings2, ShieldCheck, Sparkles, Sun, UserSquare2, Users, X, Monitor,
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
  accent?: string
}

interface NavSection {
  label: string
  items: NavItem[]
}

const sections: NavSection[] = [
  {
    label: 'Workspace',
    items: [
      { to: '/',             label: 'Dashboard', icon: LayoutDashboard, visible: () => true, exact: true, accent: 'text-accent-violet' },
      { to: '/companies',    label: 'Companies', icon: Building2,       visible: ({ canRead }) => canRead, accent: 'text-accent-teal' },
      { to: '/companies/map',label: 'Map',        icon: MapPin,          visible: ({ canRead }) => canRead, accent: 'text-accent-amber' },
    ],
  },
  {
    label: 'Data',
    items: [
      { to: '/imports', label: 'Imports', icon: FileUp,      visible: ({ canMutate }) => canMutate, accent: 'text-accent-violet' },
      { to: '/verify',  label: 'Verify',  icon: ShieldCheck, visible: ({ canRead }) => canRead, accent: 'text-accent-emerald' },
      { to: '/enrichment/jobs', label: 'Enrichment', icon: Layers, visible: ({ canRead }) => canRead, accent: 'text-accent-sky' },
    ],
  },
  {
    label: 'Settings',
    items: [
      { to: '/settings/users',            label: 'Users & Roles',      icon: Users,        visible: ({ canConfigure }) => canConfigure, accent: 'text-accent-rose' },
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
  collapsed: boolean
  onToggleCollapse: () => void
}

export function AppSidebar({ mobileOpen, onMobileClose, collapsed, onToggleCollapse }: Props) {
  const location = useLocation()
  const permissions = usePermissions()
  const { resolved, toggle } = useTheme()

  const visibleSections = useMemo(
    () =>
      sections
        .map((s) => ({ ...s, items: s.items.filter((i) => i.visible(permissions)) }))
        .filter((s) => s.items.length > 0),
    [permissions],
  )

  const isActive = (item: NavItem) =>
    item.exact ? location.pathname === item.to : location.pathname.startsWith(item.to)

  const sidebarWidth = collapsed ? 'w-[68px]' : 'w-[260px]'

  return (
    <>
      {/* Mobile overlay */}
      <div
        className={cn(
          'fixed inset-0 z-40 bg-foreground/30 backdrop-blur-sm transition-opacity duration-300 lg:hidden',
          mobileOpen ? 'opacity-100' : 'pointer-events-none opacity-0',
        )}
        aria-hidden="true"
        onClick={onMobileClose}
      />

      <aside
        className={cn(
          'fixed z-50 flex h-svh shrink-0 flex-col bg-sidebar border-r border-sidebar-border transition-all duration-300 ease-out',
          'lg:sticky lg:top-0 lg:translate-x-0',
          sidebarWidth,
          mobileOpen ? 'translate-x-0 w-[260px]' : '-translate-x-full lg:translate-x-0',
        )}
        aria-label="Primary navigation"
      >
        {/* Brand header */}
        <div className={cn(
          'flex shrink-0 items-center border-b border-sidebar-border transition-all duration-300',
          collapsed ? 'h-16 justify-center px-2' : 'h-16 justify-between px-4',
        )}>
          <Link to="/" className="flex items-center gap-2.5 group" onClick={onMobileClose}>
            <span className="flex size-9 items-center justify-center rounded-xl bg-brand-gradient text-white shadow-primary transition-transform duration-200 group-hover:scale-105">
              <Sparkles className="size-4.5" />
            </span>
            {!collapsed && (
              <div className="leading-tight animate-fade-in">
                <div className="text-sm font-bold tracking-tight text-brand-gradient">ProspectSoul</div>
                <div className="text-[10px] font-medium uppercase tracking-widest text-sidebar-muted">Intelligence</div>
              </div>
            )}
          </Link>
          {!collapsed && (
            <button
              type="button"
              className="lg:hidden inline-flex size-8 items-center justify-center rounded-lg text-sidebar-muted hover:bg-sidebar-accent hover:text-sidebar-foreground transition-colors"
              onClick={onMobileClose}
              aria-label="Close navigation"
            >
              <X className="size-4" />
            </button>
          )}
        </div>

        {/* Navigation sections */}
        <nav className={cn('flex-1 overflow-y-auto py-4', collapsed ? 'px-2' : 'px-3')}>
          {visibleSections.map((section) => (
            <div key={section.label} className="mb-6 last:mb-0">
              {!collapsed && (
                <div className="mb-2 px-3 text-[10px] font-semibold uppercase tracking-[0.12em] text-sidebar-muted">
                  {section.label}
                </div>
              )}
              <div className="flex flex-col gap-0.5">
                {section.items.map((item) => {
                  const active = isActive(item)
                  return (
                    <Link
                      key={item.to}
                      to={item.to}
                      onClick={onMobileClose}
                      title={collapsed ? item.label : undefined}
                      className={cn(
                        'group relative flex items-center rounded-lg text-[13px] font-medium transition-all duration-200',
                        collapsed ? 'justify-center px-0 py-2.5' : 'gap-2.5 px-3 py-2',
                        active
                          ? 'bg-primary/10 text-primary shadow-sm'
                          : 'text-sidebar-foreground hover:bg-sidebar-accent hover:text-foreground',
                      )}
                    >
                      {active && (
                        <span
                          aria-hidden="true"
                          className="absolute left-0 top-1.5 bottom-1.5 w-[3px] rounded-r-full bg-primary animate-scale-in"
                        />
                      )}
                      <item.icon className={cn(
                        'shrink-0 transition-colors duration-200',
                        collapsed ? 'size-5' : 'size-4',
                        active
                          ? 'text-primary'
                          : cn('text-sidebar-muted group-hover:text-foreground', item.accent && `group-hover:${item.accent}`),
                      )} />
                      {!collapsed && (
                        <span className="truncate">{item.label}</span>
                      )}
                    </Link>
                  )
                })}
              </div>
            </div>
          ))}
        </nav>

        {/* Bottom controls */}
        <div className={cn(
          'border-t border-sidebar-border',
          collapsed ? 'p-2 space-y-1' : 'p-3 space-y-2',
        )}>
          {/* Theme toggle */}
          <button
            type="button"
            onClick={toggle}
            className={cn(
              'flex items-center rounded-lg text-[13px] font-medium text-sidebar-foreground transition-all duration-200 hover:bg-sidebar-accent w-full',
              collapsed ? 'justify-center p-2.5' : 'gap-2.5 px-3 py-2',
            )}
            title={resolved === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
          >
            {resolved === 'dark' ? (
              <Sun className={cn('shrink-0 text-accent-amber', collapsed ? 'size-5' : 'size-4')} />
            ) : (
              <Moon className={cn('shrink-0 text-accent-violet', collapsed ? 'size-5' : 'size-4')} />
            )}
            {!collapsed && (
              <span>{resolved === 'dark' ? 'Light mode' : 'Dark mode'}</span>
            )}
          </button>

          {/* Collapse toggle (desktop only) */}
          <button
            type="button"
            onClick={onToggleCollapse}
            className={cn(
              'hidden lg:flex items-center rounded-lg text-[13px] font-medium text-sidebar-muted transition-all duration-200 hover:bg-sidebar-accent hover:text-sidebar-foreground w-full',
              collapsed ? 'justify-center p-2.5' : 'gap-2.5 px-3 py-2',
            )}
            title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          >
            <ChevronLeft className={cn(
              'shrink-0 transition-transform duration-300',
              collapsed ? 'size-5 rotate-180' : 'size-4',
            )} />
            {!collapsed && <span>Collapse</span>}
          </button>

          {/* Footer badge */}
          {!collapsed && (
            <div className="relative overflow-hidden rounded-xl p-3 text-xs text-white bg-brand-gradient shadow-primary">
              <div className="absolute -right-6 -bottom-6 size-20 rounded-full bg-white/10 blur-2xl" aria-hidden="true" />
              <div className="absolute -left-4 -top-4 size-16 rounded-full bg-white/5 blur-xl" aria-hidden="true" />
              <div className="relative flex items-center gap-2">
                <Monitor className="size-3.5 text-white/70" />
                <span className="font-semibold">ProspectSoul v1.0</span>
              </div>
              <div className="relative text-white/60 mt-0.5 ml-5.5">Vyoog Information Pvt Ltd</div>
            </div>
          )}
        </div>
      </aside>
    </>
  )
}

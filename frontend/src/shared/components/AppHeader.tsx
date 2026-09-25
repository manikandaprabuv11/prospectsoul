import { Button } from '@/components/ui/button'
import { Bell, Menu, Search } from 'lucide-react'
import { Link, useLocation } from 'react-router'
import { useMemo } from 'react'
import { UserMenu } from './UserMenu'

interface Props {
  onOpenMobileNav: () => void
}

function humanise(segment: string) {
  if (!segment) return ''
  const known: Record<string, string> = {
    nic: 'NIC',
    id: 'ID',
    'nic-codes': 'NIC Codes',
    'contact-roles': 'Contact Roles',
    'users': 'Users & Roles',
    'map': 'Map',
    'company-defaults': 'Company Defaults',
    'enrichment-providers': 'Enrichment Providers',
  }
  if (known[segment]) return known[segment]
  return segment
    .replace(/-/g, ' ')
    .replace(/\b\w/g, (c) => c.toUpperCase())
}

export function AppHeader({ onOpenMobileNav }: Props) {
  const location = useLocation()

  const crumbs = useMemo(() => {
    const parts = location.pathname.split('/').filter(Boolean)
    const items: { to: string; label: string }[] = [{ to: '/', label: 'Home' }]
    let path = ''
    for (const p of parts) {
      path += '/' + p
      const isUuid = /^[0-9a-f-]{36}$/i.test(p)
      items.push({ to: path, label: isUuid ? 'Detail' : humanise(p) })
    }
    return items
  }, [location.pathname])

  return (
    <header
      className="sticky top-0 z-30 flex h-14 shrink-0 items-center gap-3 border-b border-border bg-background/80 backdrop-blur-xl backdrop-saturate-150 px-4 sm:px-6"
      role="banner"
    >
      <Button
        variant="ghost"
        size="icon-sm"
        className="lg:hidden"
        onClick={onOpenMobileNav}
        aria-label="Open navigation"
      >
        <Menu className="size-5" />
      </Button>

      {/* Breadcrumb */}
      <nav aria-label="Breadcrumb" className="min-w-0 flex-1 hidden sm:block">
        <ol className="flex items-center gap-1 text-sm">
          {crumbs.map((c, i) => {
            const last = i === crumbs.length - 1
            return (
              <li key={c.to} className="flex items-center gap-1 min-w-0">
                {i > 0 && (
                  <svg className="size-3.5 text-muted-foreground/40 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path d="M9 5l7 7-7 7" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                )}
                {last ? (
                  <span className="font-medium text-foreground truncate">{c.label}</span>
                ) : (
                  <Link
                    to={c.to}
                    className="text-muted-foreground hover:text-foreground transition-colors duration-200 truncate"
                  >
                    {c.label}
                  </Link>
                )}
              </li>
            )
          })}
        </ol>
      </nav>

      {/* Global search */}
      <div className="relative hidden md:block">
        <Search
          className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground/60"
          aria-hidden="true"
        />
        <input
          type="search"
          placeholder="Search anything…"
          className="h-9 w-60 rounded-lg border border-input bg-surface-1 pl-9 pr-12 text-sm placeholder:text-muted-foreground/50 focus:outline-none focus:ring-2 focus:ring-ring/30 focus:border-ring transition-all duration-200 hover:border-border-strong"
        />
        <kbd className="pointer-events-none absolute right-2 top-1/2 -translate-y-1/2 rounded-md border border-border bg-muted/60 px-1.5 py-0.5 text-[10px] font-medium text-muted-foreground/70">
          ⌘K
        </kbd>
      </div>

      {/* Notifications stub */}
      <Button variant="ghost" size="icon-sm" className="relative text-muted-foreground hover:text-foreground">
        <Bell className="size-4" />
        <span className="absolute -top-0.5 -right-0.5 flex size-2">
          <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-accent-rose opacity-75" />
          <span className="relative inline-flex size-2 rounded-full bg-accent-rose" />
        </span>
      </Button>

      <div className="h-6 w-px bg-border hidden md:block" />

      <UserMenu />
    </header>
  )
}

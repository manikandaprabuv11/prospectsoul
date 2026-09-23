import { Button } from '@/components/ui/button'
import { Menu, Search } from 'lucide-react'
import { Link, useLocation } from 'react-router'
import { useMemo } from 'react'
import { UserMenu } from './UserMenu'

interface Props {
  onOpenMobileNav: () => void
}

/** Format a path segment into human title-case ("nic-codes" → "NIC Codes"). */
function humanise(segment: string) {
  if (!segment) return ''
  const known: Record<string, string> = {
    nic: 'NIC',
    id: 'ID',
    'nic-codes': 'NIC Codes',
    'contact-roles': 'Contact Roles',
    'users': 'Users & Roles',
    'map': 'Map',
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
      // Skip UUID-shaped segments — replace with a generic ID crumb.
      const isUuid = /^[0-9a-f-]{36}$/i.test(p)
      items.push({ to: path, label: isUuid ? 'Detail' : humanise(p) })
    }
    return items
  }, [location.pathname])

  return (
    <header
      className="sticky top-0 z-30 flex h-14 shrink-0 items-center gap-3 border-b border-border/70 bg-background/85 px-4 backdrop-blur-md sm:px-6"
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

      {/* Breadcrumb — hidden on very narrow */}
      <nav aria-label="Breadcrumb" className="min-w-0 flex-1 hidden sm:block">
        <ol className="flex items-center gap-1.5 text-sm">
          {crumbs.map((c, i) => {
            const last = i === crumbs.length - 1
            return (
              <li key={c.to} className="flex items-center gap-1.5 min-w-0">
                {i > 0 ? <span className="text-muted-foreground/50">/</span> : null}
                {last ? (
                  <span className="font-medium text-foreground truncate">{c.label}</span>
                ) : (
                  <Link
                    to={c.to}
                    className="text-muted-foreground hover:text-foreground transition-colors truncate"
                  >
                    {c.label}
                  </Link>
                )}
              </li>
            )
          })}
        </ol>
      </nav>

      {/* Global search stub — kept visually so the shell feels complete */}
      <div className="relative hidden md:block">
        <Search
          className="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground/70"
          aria-hidden="true"
        />
        <input
          type="search"
          placeholder="Search…"
          className="h-8 w-56 rounded-md border border-input bg-background pl-8 pr-3 text-sm placeholder:text-muted-foreground/70 focus:outline-none focus:ring-2 focus:ring-ring/40"
        />
        <kbd className="pointer-events-none absolute right-1.5 top-1/2 -translate-y-1/2 rounded border border-border bg-muted px-1.5 py-0.5 text-[10px] font-medium text-muted-foreground">
          ⌘K
        </kbd>
      </div>

      <UserMenu />
    </header>
  )
}

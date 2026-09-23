import { AppSidebar } from '@/shared/components/AppSidebar'
import { AppHeader } from '@/shared/components/AppHeader'
import { useState } from 'react'
import { Outlet } from 'react-router'

/**
 * Main authenticated shell.
 *
 *   ┌───────────────────────────────────────┐
 *   │ Header (breadcrumb + search + user)   │
 *   ├──────┬────────────────────────────────┤
 *   │ Nav  │ Main content                   │
 *   │      │  (max-w-7xl for readability)   │
 *   └──────┴────────────────────────────────┘
 *
 * On narrow viewports the sidebar collapses into a slide-over drawer
 * so tables and forms get the full width. All state lives here so the
 * header and sidebar can coordinate the mobile drawer.
 */
export function AppLayout() {
  const [mobileNavOpen, setMobileNavOpen] = useState(false)

  return (
    <div className="flex min-h-svh bg-background text-foreground">
      <AppSidebar mobileOpen={mobileNavOpen} onMobileClose={() => setMobileNavOpen(false)} />

      <div className="flex min-w-0 flex-1 flex-col">
        <AppHeader onOpenMobileNav={() => setMobileNavOpen(true)} />
        <main className="flex-1 overflow-y-auto">
          <div className="mx-auto w-full max-w-7xl px-4 py-6 sm:px-6 lg:px-8 lg:py-8">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}

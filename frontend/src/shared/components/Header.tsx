import { Search } from 'lucide-react'
import { UserMenu } from './UserMenu'

export function Header() {
  return (
    <header className="flex h-14 shrink-0 items-center justify-between border-b bg-card px-6">
      <div className="flex items-center gap-3">
        <h1 className="text-lg font-semibold tracking-tight">ProspectSoul</h1>
      </div>
      <div className="flex items-center gap-4">
        <div className="relative">
          <Search className="absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="search"
            placeholder="Search..."
            className="h-8 w-64 rounded-md border border-input bg-transparent pl-8 pr-3 text-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
          />
        </div>
        <UserMenu />
      </div>
    </header>
  )
}

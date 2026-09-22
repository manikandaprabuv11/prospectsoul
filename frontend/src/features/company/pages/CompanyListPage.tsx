import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Download, Plus } from 'lucide-react'
import { useState } from 'react'
import { useAuth } from '@/auth/useAuth'
import { DownloadModal } from '../components/DownloadModal'
import { Link } from 'react-router'
import { CompanyFilters } from '../components/CompanyFilters'
import { CompanyTable } from '../components/CompanyTable'
import { useCompanies } from '../hooks'
import type { CompanyFilters as Filters } from '../types'

export function CompanyListPage() {
  const [filters, setFilters] = useState<Filters>({
    page: 0,
    size: 25,
    sort: 'createdAt',
    sort_dir: 'desc',
  })

  const { data, isLoading, isError, error } = useCompanies(filters)
  const [downloadOpen, setDownloadOpen] = useState(false)
  const auth = useAuth()

  const handleSort = (field: string) => {
    setFilters((prev) => ({
      ...prev,
      sort: field,
      sort_dir: prev.sort === field && prev.sort_dir === 'desc' ? 'asc' : 'desc',
    }))
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Companies</h1>
          <p className="text-sm text-muted-foreground">
            {data ? `${data.total_elements} companies` : 'Manage prospect companies'}
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => setDownloadOpen(true)}>
            <Download className="size-4" /> Download
          </Button>
          <Button variant="outline" asChild>
            <Link to="/imports/new">Import</Link>
          </Button>
          <Button asChild>
            <Link to="/companies/new"><Plus className="size-4" /> New Company</Link>
          </Button>
        </div>
      </div>

      <CompanyFilters filters={filters} onChange={setFilters} />

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full" />
          ))}
        </div>
      ) : isError ? (
        <div className="rounded-lg border border-destructive/50 p-6 text-center text-sm text-destructive">
          {error instanceof Error ? error.message : 'Failed to load companies'}
        </div>
      ) : data ? (
        <>
          <CompanyTable
            companies={data.content}
            onSort={handleSort}
            sortField={filters.sort ?? 'createdAt'}
            sortDir={filters.sort_dir ?? 'desc'}
          />
          {data.total_pages > 1 && (
            <div className="flex items-center justify-between">
              <p className="text-sm text-muted-foreground">
                Page {data.page + 1} of {data.total_pages} ({data.total_elements} total)
              </p>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={data.page === 0}
                  onClick={() => setFilters((f) => ({ ...f, page: (f.page ?? 0) - 1 }))}
                >
                  Previous
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={data.page >= data.total_pages - 1}
                  onClick={() => setFilters((f) => ({ ...f, page: (f.page ?? 0) + 1 }))}
                >
                  Next
                </Button>
              </div>
            </div>
          )}
        </>
      ) : null}
      <DownloadModal
        open={downloadOpen}
        onOpenChange={setDownloadOpen}
        filters={filters}
        matchesCount={data?.total_elements}
        authHeader={auth.token ? `Bearer ${auth.token}` : undefined}
      />
    </div>
  )
}

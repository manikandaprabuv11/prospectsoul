import { Button } from '@/components/ui/button'
import { LoadingRows } from '@/components/feedback/LoadingRows'
import { ErrorState } from '@/components/feedback/ErrorState'
import { PageHeader } from '@/components/layout/PageHeader'
import { Download, FileUp, Plus } from 'lucide-react'
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

  const pageStart = data ? (data.page ?? 0) * (data.size ?? 25) + 1 : 0
  const pageEnd   = data ? Math.min((data.page ?? 0) * (data.size ?? 25) + data.content.length, data.total_elements) : 0

  return (
    <div className="space-y-6">
      <PageHeader
        title="Companies"
        description={
          data
            ? `${data.total_elements.toLocaleString()} prospects across your pipeline.`
            : 'Manage prospect companies, filter, verify and export.'
        }
        actions={
          <>
            <Button variant="outline" onClick={() => setDownloadOpen(true)}>
              <Download /> Download
            </Button>
            <Button variant="outline" asChild>
              <Link to="/imports/new"><FileUp /> Import</Link>
            </Button>
            <Button asChild>
              <Link to="/companies/new"><Plus /> New company</Link>
            </Button>
          </>
        }
      />

      <div className="rounded-lg border border-border/70 bg-card p-4 shadow-xs">
        <CompanyFilters filters={filters} onChange={setFilters} />
      </div>

      {isLoading ? (
        <LoadingRows count={6} />
      ) : isError ? (
        <ErrorState message={error instanceof Error ? error.message : 'Failed to load companies'} />
      ) : data ? (
        <>
          <CompanyTable
            companies={data.content}
            onSort={handleSort}
            sortField={filters.sort ?? 'createdAt'}
            sortDir={filters.sort_dir ?? 'desc'}
          />
          {data.total_pages > 1 && (
            <div className="flex flex-col-reverse gap-3 sm:flex-row sm:items-center sm:justify-between text-sm">
              <p className="text-muted-foreground" data-tabular="true">
                Showing <span className="font-medium text-foreground">{pageStart}</span>–
                <span className="font-medium text-foreground">{pageEnd}</span> of{' '}
                <span className="font-medium text-foreground">{data.total_elements.toLocaleString()}</span>
              </p>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={data.page === 0}
                  onClick={() => setFilters((f) => ({ ...f, page: (f.page ?? 0) - 1 }))}
                >
                  Previous
                </Button>
                <span className="text-xs text-muted-foreground tabular-nums px-1">
                  Page {data.page + 1} of {data.total_pages}
                </span>
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

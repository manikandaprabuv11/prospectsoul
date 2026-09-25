import { Button } from '@/components/ui/button'
import { LoadingRows } from '@/components/feedback/LoadingRows'
import { ErrorState } from '@/components/feedback/ErrorState'
import { PageHeader } from '@/components/layout/PageHeader'
import { Pagination } from '@/shared/components/Pagination'
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

      <div className="rounded-xl border border-border bg-card p-4 shadow-card">
        <CompanyFilters filters={filters} onChange={setFilters} />
      </div>

      {isLoading ? (
        <LoadingRows count={6} />
      ) : isError ? (
        <ErrorState message={error instanceof Error ? error.message : 'Failed to load companies'} />
      ) : data ? (
        <div className="space-y-4">
          <CompanyTable
            companies={data.content}
            onSort={handleSort}
            sortField={filters.sort ?? 'createdAt'}
            sortDir={filters.sort_dir ?? 'desc'}
          />
          <Pagination
            page={data.page}
            totalPages={data.total_pages}
            totalElements={data.total_elements}
            itemLabel="companies"
            onPageChange={(page) => setFilters((f) => ({ ...f, page }))}
          />
        </div>
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

import { Button } from '@/components/ui/button'
import { LoadingRows } from '@/components/feedback/LoadingRows'
import { ErrorState } from '@/components/feedback/ErrorState'
import { PageHeader } from '@/components/layout/PageHeader'
import { Pagination } from '@/shared/components/Pagination'
import { Download, FileUp, Plus } from 'lucide-react'
import { useCallback, useState } from 'react'
import { useAuth } from '@/auth/useAuth'
import { DownloadModal } from '../components/DownloadModal'
import { Link, useSearchParams } from 'react-router'
import { CompanyFilters } from '../components/CompanyFilters'
import { CompanyTable } from '../components/CompanyTable'
import { useCompanies } from '../hooks'
import type { CompanyFilters as Filters, ConfidenceLevel, NumberSourceType } from '../types'

const DEFAULTS: Filters = { page: 0, size: 25, sort: 'createdAt', sort_dir: 'desc' }

function filtersFromParams(params: URLSearchParams): Filters {
  const str = (key: string) => params.get(key) || undefined
  const num = (key: string) => { const v = params.get(key); return v ? Number(v) : undefined }
  const bool = (key: string) => {
    const v = params.get(key)
    if (v === 'true') return true
    if (v === 'false') return false
    return undefined
  }

  return {
    q: str('q'),
    city: str('city'),
    state: str('state'),
    industry: str('industry'),
    cluster: str('cluster'),
    source: str('source'),
    pipeline_state: str('pipeline_state'),
    verification_status: str('verification_status'),
    region: str('region'),
    district: str('district'),
    pincode: str('pincode'),
    turnover_min: str('turnover_min'),
    turnover_max: str('turnover_max'),
    employee_min: str('employee_min'),
    employee_max: str('employee_max'),
    gst_present: bool('gst_present'),
    nic_code_id: str('nic_code_id'),
    nic_parent_ids: params.getAll('nic_parent_ids').length ? params.getAll('nic_parent_ids') : undefined,
    nic_include_descendants: bool('nic_include_descendants'),
    has_contact_role_id: str('has_contact_role_id'),
    confidence: str('confidence') as ConfidenceLevel | undefined,
    number_source: str('number_source') as NumberSourceType | undefined,
    has_decision_maker: bool('has_decision_maker'),
    view: str('view') as Filters['view'],
    page: num('page') ?? DEFAULTS.page,
    size: num('size') ?? DEFAULTS.size,
    sort: str('sort') ?? DEFAULTS.sort,
    sort_dir: str('sort_dir') ?? DEFAULTS.sort_dir,
  }
}

function filtersToParams(f: Filters): URLSearchParams {
  const p = new URLSearchParams()
  const set = (key: string, val: string | number | boolean | undefined | null) => {
    if (val !== undefined && val !== null && val !== '') p.set(key, String(val))
  }
  set('q', f.q)
  set('city', f.city)
  set('state', f.state)
  set('industry', f.industry)
  set('cluster', f.cluster)
  set('source', f.source)
  set('pipeline_state', f.pipeline_state)
  set('verification_status', f.verification_status)
  set('region', f.region)
  set('district', f.district)
  set('pincode', f.pincode)
  set('turnover_min', f.turnover_min)
  set('turnover_max', f.turnover_max)
  set('employee_min', f.employee_min)
  set('employee_max', f.employee_max)
  if (f.gst_present !== undefined) set('gst_present', f.gst_present)
  set('nic_code_id', f.nic_code_id)
  f.nic_parent_ids?.forEach((id) => p.append('nic_parent_ids', id))
  if (f.nic_include_descendants === false) set('nic_include_descendants', false)
  set('has_contact_role_id', f.has_contact_role_id)
  set('confidence', f.confidence)
  set('number_source', f.number_source)
  if (f.has_decision_maker !== undefined) set('has_decision_maker', f.has_decision_maker)
  set('view', f.view)
  if (f.page && f.page !== DEFAULTS.page) set('page', f.page)
  if (f.size && f.size !== DEFAULTS.size) set('size', f.size)
  if (f.sort && f.sort !== DEFAULTS.sort) set('sort', f.sort)
  if (f.sort_dir && f.sort_dir !== DEFAULTS.sort_dir) set('sort_dir', f.sort_dir)
  return p
}

export function CompanyListPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const filters = filtersFromParams(searchParams)

  const setFilters = useCallback((next: Filters | ((prev: Filters) => Filters)) => {
    setSearchParams((prev) => {
      const current = filtersFromParams(prev)
      const resolved = typeof next === 'function' ? next(current) : next
      return filtersToParams(resolved)
    }, { replace: true })
  }, [setSearchParams])

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

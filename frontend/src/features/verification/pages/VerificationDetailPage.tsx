import { Button } from '@/components/ui/button'
import { PageHeader } from '@/components/layout/PageHeader'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Progress } from '@/components/ui/progress'
import { Select } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { ArrowLeft } from 'lucide-react'
import { useState } from 'react'
import { Link, useParams } from 'react-router'
import {
  formatDate,
  formatDateTime,
  formatElapsed,
  formatFailureCode,
  formatLineType,
  formatPhone,
  problemDetail,
} from '../components/format'
import { BatchStatusBadge, ItemStatusBadge } from '../components/VerificationStatusBadge'
import { useVerificationBatch, useVerificationItems } from '../hooks'

const PAGE_SIZE = 25
const ITEM_STATUSES = ['QUEUED', 'PROCESSING', 'VERIFIED', 'FAILED', 'SKIPPED'] as const

/**
 * `/verify/:id` — one job's summary statistics and its per-company results,
 * including the reason every failure or skip happened.
 */
export function VerificationDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [status, setStatus] = useState('')
  const [search, setSearch] = useState('')
  const [appliedSearch, setAppliedSearch] = useState<string | undefined>(undefined)
  const [page, setPage] = useState(0)

  const batch = useVerificationBatch(id)
  const items = useVerificationItems(id, {
    status: status || undefined,
    q: appliedSearch,
    page,
    size: PAGE_SIZE,
    sort: 'createdAt',
    sort_dir: 'asc',
  })

  if (batch.isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-40 w-full" />
        <Skeleton className="h-64 w-full" />
      </div>
    )
  }

  if (batch.isError) {
    const message = problemDetail(batch.error, 'Could not load this verification job.')
    return (
      <div className="space-y-4">
        <BackLink />
        <div
          role="alert"
          className="rounded-lg border border-destructive/50 p-8 text-center text-sm text-destructive"
        >
          {message}
        </div>
      </div>
    )
  }

  const job = batch.data
  if (!job) return null

  const rows = items.data?.content ?? []

  return (
    <div className="space-y-6">
      <PageHeader
        breadcrumb={<Link to="/verify" className="inline-flex items-center gap-1 hover:text-foreground transition-colors"><ArrowLeft className="size-3.5" /> Back to verify</Link>}
        title={<span className="flex items-center gap-3 flex-wrap">Verification job <BatchStatusBadge status={job.status} /></span>}
        description={<>Started {formatDateTime(job.started_at ?? job.created_at)} by {job.requested_by_name ?? job.requested_by}</>}
      />

      <Card>
        <CardHeader className="pb-3">
          <CardTitle>Summary</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <div className="flex items-baseline justify-between">
              <p className="text-sm font-medium">
                {job.verified_count + job.failed_count + job.skipped_count} / {job.total_count}{' '}
                companies processed
              </p>
              <p className="text-sm font-semibold tabular-nums">{job.progress_percent}%</p>
            </div>
            <Progress value={job.progress_percent} label="Job progress" />
          </div>

          <dl className="grid grid-cols-2 gap-4 sm:grid-cols-4 lg:grid-cols-7">
            <Stat label="Total" value={job.total_count} />
            <Stat label="Verified" value={job.verified_count} className="text-emerald-600" />
            <Stat label="Failed" value={job.failed_count} className="text-red-600" />
            <Stat label="Skipped" value={job.skipped_count} />
            <Stat label="Queued" value={job.queued_count} />
            <Stat label="Processing" value={job.processing_count} className="text-amber-600" />
            <div>
              <dt className="text-xs tracking-wide text-muted-foreground uppercase">Duration</dt>
              <dd className="text-lg font-semibold tabular-nums">
                {formatElapsed(job.elapsed_seconds)}
              </dd>
            </div>
          </dl>

          <dl className="grid gap-x-6 gap-y-2 border-t pt-4 text-sm sm:grid-cols-3">
            <div>
              <dt className="text-muted-foreground">Filter — added by</dt>
              <dd>{job.filter_added_by_name ?? (job.filter_added_by ? job.filter_added_by : 'All users')}</dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Filter — date range</dt>
              <dd>
                {job.filter_date_from || job.filter_date_to
                  ? `${formatDate(job.filter_date_from)} – ${formatDate(job.filter_date_to)}`
                  : 'Any date'}
              </dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Completed</dt>
              <dd>{formatDateTime(job.completed_at)}</dd>
            </div>
          </dl>
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="pb-3">
          <CardTitle>Results</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-wrap items-end gap-3">
            <div className="space-y-1">
              <Label htmlFor="item-status">Status</Label>
              <Select
                id="item-status"
                className="w-44"
                value={status}
                onChange={(event) => {
                  setStatus(event.target.value)
                  setPage(0)
                }}
              >
                <option value="">All results</option>
                {ITEM_STATUSES.map((value) => (
                  <option key={value} value={value}>
                    {value.charAt(0) + value.slice(1).toLowerCase()}
                  </option>
                ))}
              </Select>
            </div>
            <div className="min-w-48 flex-1 space-y-1">
              <Label htmlFor="item-search">Search</Label>
              <Input
                id="item-search"
                placeholder="Company or phone"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter') {
                    setAppliedSearch(search.trim() || undefined)
                    setPage(0)
                  }
                }}
              />
            </div>
            <Button
              onClick={() => {
                setAppliedSearch(search.trim() || undefined)
                setPage(0)
              }}
            >
              Search
            </Button>
          </div>

          {items.isLoading ? (
            <div className="space-y-2">
              {Array.from({ length: 5 }).map((_, index) => (
                <Skeleton key={index} className="h-11 w-full" />
              ))}
            </div>
          ) : items.isError ? (
            <div
              role="alert"
              className="rounded-lg border border-destructive/50 p-6 text-center text-sm text-destructive"
            >
              {problemDetail(items.error, 'Could not load results.')}
            </div>
          ) : rows.length === 0 ? (
            <div className="rounded-lg border p-10 text-center">
              <p className="font-medium">No results match this filter</p>
              <p className="mt-1 text-sm text-muted-foreground">
                Clear the status filter or the search term to see every company in this job.
              </p>
            </div>
          ) : (
            <>
              <div className="overflow-x-auto rounded-lg border">
                <table className="w-full text-sm">
                  <caption className="sr-only">Per-company verification results</caption>
                  <thead>
                    <tr className="border-b bg-muted/50">
                      <th scope="col" className="px-3 py-2.5 text-left font-medium text-muted-foreground">Company</th>
                      <th scope="col" className="px-3 py-2.5 text-left font-medium text-muted-foreground">Status</th>
                      <th scope="col" className="px-3 py-2.5 text-left font-medium text-muted-foreground">Phone</th>
                      <th scope="col" className="px-3 py-2.5 text-left font-medium text-muted-foreground">Line type</th>
                      <th scope="col" className="px-3 py-2.5 text-left font-medium text-muted-foreground">Carrier</th>
                      <th scope="col" className="px-3 py-2.5 text-left font-medium text-muted-foreground">Reason</th>
                      <th scope="col" className="px-3 py-2.5 text-right font-medium text-muted-foreground">Attempts</th>
                      <th scope="col" className="px-3 py-2.5 text-left font-medium text-muted-foreground">Completed</th>
                    </tr>
                  </thead>
                  <tbody>
                    {rows.map((item) => (
                      <tr key={item.id} className="border-b last:border-0 hover:bg-muted/30">
                        <td className="px-3 py-2.5 font-medium">
                          <Link to={`/companies/${item.company_id}`} className="text-primary hover:underline">
                            {item.company_name ?? item.company_id}
                          </Link>
                        </td>
                        <td className="px-3 py-2.5">
                          <ItemStatusBadge status={item.status} />
                        </td>
                        <td className="px-3 py-2.5">
                          {formatPhone(item.normalized_phone_number ?? item.phone_number)}
                        </td>
                        <td className="px-3 py-2.5">{formatLineType(item.line_type)}</td>
                        <td className="px-3 py-2.5">{item.carrier_name ?? '—'}</td>
                        <td className="px-3 py-2.5">
                          {item.failure_code ? (
                            <span title={item.failure_message ?? undefined}>
                              {formatFailureCode(item.failure_code)}
                            </span>
                          ) : (
                            '—'
                          )}
                        </td>
                        <td className="px-3 py-2.5 text-right tabular-nums">{item.attempt_count}</td>
                        <td className="px-3 py-2.5 text-muted-foreground">
                          {formatDateTime(item.completed_at)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="flex items-center justify-between">
                <p className="text-sm text-muted-foreground">
                  Page {(items.data?.page ?? 0) + 1} of {Math.max(1, items.data?.total_pages ?? 1)}
                  {' · '}
                  {items.data?.total_elements ?? 0} results
                </p>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page === 0}
                    onClick={() => setPage((current) => current - 1)}
                  >
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page >= (items.data?.total_pages ?? 1) - 1}
                    onClick={() => setPage((current) => current + 1)}
                  >
                    Next
                  </Button>
                </div>
              </div>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

function BackLink() {
  return (
    <Link
      to="/verify"
      className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
    >
      <ArrowLeft className="size-4" />
      Back to Verify
    </Link>
  )
}

function Stat({ label, value, className }: { label: string; value: number; className?: string }) {
  return (
    <div>
      <dt className="text-xs tracking-wide text-muted-foreground uppercase">{label}</dt>
      <dd className={`text-lg font-semibold tabular-nums ${className ?? ''}`}>{value}</dd>
    </div>
  )
}

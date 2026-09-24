import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Select } from '@/components/ui/select'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import { Pagination } from '@/shared/components/Pagination'
import { useState } from 'react'
import { Link } from 'react-router'
import { useAddedByOptions, useVerificationBatches } from '../hooks'
import type { VerificationBatch } from '../types'
import { formatDate, formatDateTime, formatElapsed, problemDetail } from './format'
import { BatchStatusBadge } from './VerificationStatusBadge'

const PAGE_SIZE = 10

const STATUSES = [
  'QUEUED',
  'PROCESSING',
  'COMPLETED',
  'COMPLETED_WITH_ERRORS',
  'FAILED',
  'CANCELLED',
] as const

/** "Previous Verification Jobs" — every batch, with its filter snapshot. */
export function VerificationHistoryTable() {
  const [status, setStatus] = useState('')
  const [requestedBy, setRequestedBy] = useState('')
  const [page, setPage] = useState(0)

  const requesters = useAddedByOptions()
  const batches = useVerificationBatches({
    status: status || undefined,
    requested_by: requestedBy || undefined,
    page,
    size: PAGE_SIZE,
    sort: 'createdAt',
    sort_dir: 'desc',
  })

  const rows = batches.data?.content ?? []

  return (
    <Card>
      <CardHeader className="pb-3">
        <CardTitle>Previous Verification Jobs</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex flex-wrap items-end gap-3">
          <div className="space-y-1">
            <Label htmlFor="history-status">Status</Label>
            <Select
              id="history-status"
              className="w-52"
              value={status}
              onChange={(event) => {
                setStatus(event.target.value)
                setPage(0)
              }}
            >
              <option value="">All statuses</option>
              {STATUSES.map((value) => (
                <option key={value} value={value}>
                  {value.replaceAll('_', ' ')}
                </option>
              ))}
            </Select>
          </div>
          <div className="space-y-1">
            <Label htmlFor="history-requested-by">Requested by</Label>
            <Select
              id="history-requested-by"
              className="w-48"
              value={requestedBy}
              onChange={(event) => {
                setRequestedBy(event.target.value)
                setPage(0)
              }}
            >
              <option value="">Anyone</option>
              {(requesters.data ?? []).map((option) => (
                <option key={option.id} value={option.id}>
                  {option.name}
                </option>
              ))}
            </Select>
          </div>
        </div>

        {batches.isLoading ? (
          <div className="space-y-2">
            {Array.from({ length: 3 }).map((_, index) => (
              <Skeleton key={index} className="h-11 w-full" />
            ))}
          </div>
        ) : batches.isError ? (
          <div
            role="alert"
            className="rounded-lg border border-destructive/50 p-6 text-center text-sm text-destructive"
          >
            {problemDetail(batches.error, 'Could not load verification jobs.')}
          </div>
        ) : rows.length === 0 ? (
          <div className="rounded-lg border p-10 text-center">
            <p className="font-medium">No verification jobs yet</p>
            <p className="mt-1 text-sm text-muted-foreground">
              Completed and running jobs appear here with their filters and statistics.
            </p>
          </div>
        ) : (
          <>
            <div className="overflow-x-auto rounded-lg border">
              <table className="w-full text-sm">
                <caption className="sr-only">Previous verification jobs</caption>
                <thead>
                  <tr className="border-b bg-muted/50">
                    <th scope="col" className="px-3 py-2.5 text-left font-medium text-muted-foreground">Started</th>
                    <th scope="col" className="px-3 py-2.5 text-left font-medium text-muted-foreground">Requested by</th>
                    <th scope="col" className="px-3 py-2.5 text-left font-medium text-muted-foreground">Filter</th>
                    <th scope="col" className="px-3 py-2.5 text-right font-medium text-muted-foreground">Total</th>
                    <th scope="col" className="px-3 py-2.5 text-right font-medium text-muted-foreground">Verified</th>
                    <th scope="col" className="px-3 py-2.5 text-right font-medium text-muted-foreground">Failed</th>
                    <th scope="col" className="px-3 py-2.5 text-right font-medium text-muted-foreground">Skipped</th>
                    <th scope="col" className="px-3 py-2.5 text-left font-medium text-muted-foreground">Duration</th>
                    <th scope="col" className="px-3 py-2.5 text-left font-medium text-muted-foreground">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((batch) => (
                    <tr key={batch.id} className="border-b last:border-0 hover:bg-muted/30">
                      <td className="px-3 py-2.5 font-medium">
                        <Link to={`/verify/${batch.id}`} className="text-primary hover:underline">
                          {formatDateTime(batch.started_at ?? batch.created_at)}
                        </Link>
                      </td>
                      <td className="px-3 py-2.5">{batch.requested_by_name ?? batch.requested_by}</td>
                      <td className="px-3 py-2.5 text-muted-foreground">{filterSummary(batch)}</td>
                      <td className="px-3 py-2.5 text-right tabular-nums">{batch.total_count}</td>
                      <td className="px-3 py-2.5 text-right tabular-nums text-emerald-600">{batch.verified_count}</td>
                      <td className="px-3 py-2.5 text-right tabular-nums text-red-600">{batch.failed_count}</td>
                      <td className="px-3 py-2.5 text-right tabular-nums">{batch.skipped_count}</td>
                      <td className="px-3 py-2.5 text-muted-foreground tabular-nums">
                        {formatElapsed(batch.elapsed_seconds)}
                      </td>
                      <td className="px-3 py-2.5">
                        <BatchStatusBadge status={batch.status} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <Pagination
              page={batches.data?.page ?? 0}
              totalPages={batches.data?.total_pages ?? 1}
              totalElements={batches.data?.total_elements}
              itemLabel="jobs"
              onPageChange={setPage}
            />
          </>
        )}
      </CardContent>
    </Card>
  )
}

/** The filter snapshot the batch was created under, e.g. "Mani · 01–09 Sep". */
function filterSummary(batch: VerificationBatch): string {
  const who = batch.filter_added_by_name ?? (batch.filter_added_by ? batch.filter_added_by : 'All')
  if (!batch.filter_date_from && !batch.filter_date_to) {
    return `${who} · any date`
  }
  const from = batch.filter_date_from ? formatDate(batch.filter_date_from) : 'any'
  const to = batch.filter_date_to ? formatDate(batch.filter_date_to) : 'any'
  return from === to ? `${who} · ${from}` : `${who} · ${from} – ${to}`
}

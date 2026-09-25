import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { PageHeader } from '@/components/layout/PageHeader'
import { StatCard } from '@/components/layout/StatCard'
import { LoadingRows } from '@/components/feedback/LoadingRows'
import { ErrorState } from '@/components/feedback/ErrorState'
import { EmptyState } from '@/components/feedback/EmptyState'
import { StatusChip } from '@/components/feedback/StatusChip'
import { InlineTip } from '@/components/feedback/InlineTip'
import { ArrowLeft, CheckCircle2, FileSpreadsheet, Loader2, XCircle, ListChecks } from 'lucide-react'
import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { useImportBatch, useImportRows } from '../hooks'

export function ImportDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { data: batch, isLoading, isError } = useImportBatch(id)
  const [rowPage, setRowPage] = useState(0)
  const isLive = batch?.status === 'PROCESSING'
  const { data: rows } = useImportRows(id, rowPage, 25, isLive)

  if (isLoading) {
    return (
      <div className="space-y-4">
        <LoadingRows count={1} height="h-16" />
        <LoadingRows count={4} height="h-24" />
      </div>
    )
  }
  if (isError || !batch) {
    return <ErrorState message="Import batch not found or failed to load." />
  }

  const pct = batch.total_rows > 0
    ? Math.min(100, Math.round((batch.processed_rows / batch.total_rows) * 100))
    : batch.status === 'COMPLETED' ? 100 : 0

  return (
    <div className="space-y-6">
      <PageHeader
        breadcrumb={
          <Link to="/imports" className="inline-flex items-center gap-1 hover:text-foreground transition-colors duration-200">
            <ArrowLeft className="size-3.5" /> Back to imports
          </Link>
        }
        title={
          <span className="flex items-center gap-3 flex-wrap min-w-0">
            <div className="flex size-8 items-center justify-center rounded-lg bg-accent-violet/12 text-accent-violet shrink-0">
              <FileSpreadsheet className="size-4" />
            </div>
            <span className="truncate">{batch.file_name}</span>
            <StatusChip kind="batch" value={batch.status} />
          </span>
        }
        description={`${batch.file_type} · Source: ${batch.source}`}
      />

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatCard
          label="Total rows"
          value={batch.total_rows.toLocaleString()}
          icon={<ListChecks className="size-4" />}
        />
        <StatCard
          label="Created"
          value={batch.created_rows.toLocaleString()}
          accent="emerald"
          icon={<CheckCircle2 className="size-4" />}
        />
        <StatCard
          label="Duplicates"
          value={batch.duplicate_rows.toLocaleString()}
          accent="amber"
        />
        <StatCard
          label="Rejected"
          value={batch.rejected_rows.toLocaleString()}
          accent="rose"
          icon={<XCircle className="size-4" />}
        />
      </div>

      {batch.status === 'PROCESSING' && (
        <div className="rounded-xl border border-border bg-card p-5 space-y-3 shadow-card">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 text-sm">
            <div className="flex items-center gap-2.5 min-w-0">
              <div className="relative">
                <div className="absolute inset-0 rounded-full bg-primary/20 animate-pulse-ring" />
                <Loader2 className="size-4 text-primary animate-spin relative" />
              </div>
              <span className="font-semibold">Processing</span>
              <span className="text-muted-foreground tabular-nums">
                · {batch.processed_rows.toLocaleString()} of {batch.total_rows.toLocaleString()} rows · {pct}%
              </span>
            </div>
            <div className="text-xs text-muted-foreground">
              Runs on the server — safe to close this tab.
            </div>
          </div>
          <div className="h-2.5 w-full overflow-hidden rounded-full bg-surface-1">
            <div
              className="h-full rounded-full bg-brand-gradient transition-all duration-500"
              style={{ width: `${pct}%` }}
              role="progressbar"
              aria-valuemin={0}
              aria-valuemax={100}
              aria-valuenow={pct}
              aria-label="Import progress"
            />
          </div>
        </div>
      )}

      {batch.error_message && (
        <ErrorState title="Import error" message={batch.error_message} />
      )}

      <Card>
        <CardHeader className="border-b-0 pb-2 flex-row items-center justify-between">
          <CardTitle>Rows</CardTitle>
          {isLive && (
            <span className="inline-flex items-center gap-1.5 text-xs font-medium text-primary">
              <span className="relative flex size-2">
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-primary opacity-75"></span>
                <span className="relative inline-flex size-2 rounded-full bg-primary"></span>
              </span>
              Live
            </span>
          )}
        </CardHeader>
        <CardContent className="pt-2">
          {!rows ? (
            <LoadingRows count={4} height="h-10" />
          ) : rows.content.length === 0 ? (
            <EmptyState title="No rows yet" description="Once the batch is processed, each row shows its outcome here." />
          ) : (
            <div className="overflow-hidden rounded-xl border border-border">
              <div className="overflow-x-auto">
                <table className="w-full text-sm" data-tabular="true">
                  <thead>
                    <tr className="border-b border-border bg-surface-1">
                      <Th>#</Th>
                      <Th>Status</Th>
                      <Th>Reason</Th>
                      <Th>Error</Th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border">
                    {rows.content.map((r) => (
                      <tr key={r.id} className="hover:bg-accent/30 transition-colors duration-150">
                        <td className="px-3 py-2.5 tabular-nums text-muted-foreground w-12 font-medium">{r.row_number}</td>
                        <td className="px-3 py-2.5"><StatusChip kind="row" value={r.status} /></td>
                        <td className="px-3 py-2.5 text-xs text-muted-foreground max-w-[240px] truncate">{r.outcome_reason ?? <span className="text-muted-foreground/50">—</span>}</td>
                        <td className="px-3 py-2.5 text-xs text-accent-rose max-w-[420px] truncate">{r.error_message ?? <span className="text-muted-foreground/50">—</span>}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {rows.total_pages > 1 && (
                <div className="flex items-center justify-between px-4 py-3 border-t border-border text-xs text-muted-foreground tabular-nums bg-surface-1/50">
                  <span>Page <span className="font-semibold text-foreground">{rows.page + 1}</span> of <span className="font-semibold text-foreground">{rows.total_pages}</span></span>
                  <div className="flex gap-2">
                    <Button variant="outline" size="xs" disabled={rows.page === 0} onClick={() => setRowPage((p) => p - 1)}>Prev</Button>
                    <Button variant="outline" size="xs" disabled={rows.page >= rows.total_pages - 1} onClick={() => setRowPage((p) => p + 1)}>Next</Button>
                  </div>
                </div>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      {batch.status === 'COMPLETED' && batch.rejected_rows > 0 && (
        <InlineTip tone="warning">
          {batch.rejected_rows.toLocaleString()} row(s) were skipped. Each one carries a reason above so you can fix
          the source file and re-import — the batch is idempotent.
        </InlineTip>
      )}
    </div>
  )
}

function Th({ children }: { children: React.ReactNode }) {
  return (
    <th scope="col" className="px-3 py-3 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
      {children}
    </th>
  )
}

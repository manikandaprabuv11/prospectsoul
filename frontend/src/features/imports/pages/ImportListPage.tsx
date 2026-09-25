import { Button } from '@/components/ui/button'
import { LoadingRows } from '@/components/feedback/LoadingRows'
import { ErrorState } from '@/components/feedback/ErrorState'
import { EmptyState } from '@/components/feedback/EmptyState'
import { PageHeader } from '@/components/layout/PageHeader'
import { StatusChip } from '@/components/feedback/StatusChip'
import { Pagination } from '@/shared/components/Pagination'
import { FileUp, Plus } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'
import { useImportBatches } from '../hooks'

export function ImportListPage() {
  const [page, setPage] = useState(0)
  const { data, isLoading, isError, error } = useImportBatches(page)

  return (
    <div className="space-y-6">
      <PageHeader
        title="Imports"
        description="Upload Excel or CSV files. Progress updates live; safe to close the tab."
        actions={
          <Button asChild>
            <Link to="/imports/new"><Plus /> New import</Link>
          </Button>
        }
      />

      {isLoading ? (
        <LoadingRows count={5} height="h-14" />
      ) : isError ? (
        <ErrorState message={error instanceof Error ? error.message : 'Failed to load imports'} />
      ) : data && data.content.length === 0 ? (
        <EmptyState
          icon={<FileUp className="size-6" />}
          title="No imports yet"
          description="Upload your first Excel or CSV file to get started."
          action={
            <Button asChild>
              <Link to="/imports/new">Upload a file</Link>
            </Button>
          }
        />
      ) : data ? (
        <div className="space-y-4">
          <div className="overflow-hidden rounded-xl border border-border bg-card shadow-card">
            <div className="overflow-x-auto">
              <table className="w-full text-sm" data-tabular="true">
                <thead>
                  <tr className="border-b border-border bg-surface-1">
                    <Th>File</Th>
                    <Th>Source</Th>
                    <Th>Status</Th>
                    <Th className="text-right">Rows</Th>
                    <Th className="text-right">Created</Th>
                    <Th className="text-right">Duplicates</Th>
                    <Th className="text-right">Rejected</Th>
                    <Th>Uploaded</Th>
                    <Th className="text-right pr-4">&nbsp;</Th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {data.content.map((batch) => {
                    const pct = batch.total_rows > 0
                      ? Math.round((batch.processed_rows / batch.total_rows) * 100)
                      : batch.status === 'COMPLETED' ? 100 : 0
                    return (
                      <tr key={batch.id} className="hover:bg-accent/30 transition-colors duration-150 group/row">
                        <td className="px-3 py-3 max-w-[280px]">
                          <Link
                            to={`/imports/${batch.id}`}
                            className="font-medium text-foreground hover:text-primary transition-colors duration-200 block truncate"
                          >
                            {batch.file_name}
                          </Link>
                          <p className="text-[11px] text-muted-foreground uppercase tracking-wide">{batch.file_type}</p>
                        </td>
                        <td className="px-3 py-3">
                          <span className="inline-flex items-center rounded-md bg-primary/8 px-2 py-0.5 text-[11px] font-semibold text-primary">
                            {batch.source}
                          </span>
                        </td>
                        <td className="px-3 py-3 min-w-[160px]">
                          <StatusChip kind="batch" value={batch.status} />
                          {batch.status === 'PROCESSING' && (
                            <div className="mt-2 h-1.5 w-32 overflow-hidden rounded-full bg-muted">
                              <div
                                className="h-full rounded-full bg-primary transition-all duration-500"
                                style={{ width: `${pct}%` }}
                              />
                            </div>
                          )}
                        </td>
                        <td className="px-3 py-3 text-right tabular-nums font-medium">{batch.total_rows.toLocaleString()}</td>
                        <td className="px-3 py-3 text-right tabular-nums text-accent-emerald font-semibold">
                          {batch.created_rows.toLocaleString()}
                        </td>
                        <td className="px-3 py-3 text-right tabular-nums text-accent-amber font-medium">
                          {batch.duplicate_rows.toLocaleString()}
                        </td>
                        <td className="px-3 py-3 text-right tabular-nums text-accent-rose font-medium">
                          {batch.rejected_rows.toLocaleString()}
                        </td>
                        <td className="px-3 py-3 whitespace-nowrap text-muted-foreground text-[13px]">
                          {new Date(batch.created_at).toLocaleString(undefined, {
                            year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
                          })}
                        </td>
                        <td className="px-3 py-3 text-right pr-4">
                          <Button variant="ghost" size="sm" className="opacity-60 group-hover/row:opacity-100 transition-opacity" asChild>
                            <Link to={`/imports/${batch.id}`}>Open</Link>
                          </Button>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          </div>

          <Pagination
            page={data.page}
            totalPages={data.total_pages}
            totalElements={data.total_elements}
            itemLabel="imports"
            onPageChange={setPage}
          />
        </div>
      ) : null}
    </div>
  )
}

function Th({ children, className }: { children: React.ReactNode; className?: string }) {
  return (
    <th scope="col" className={`px-3 py-3 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground whitespace-nowrap ${className ?? ''}`}>
      {children}
    </th>
  )
}

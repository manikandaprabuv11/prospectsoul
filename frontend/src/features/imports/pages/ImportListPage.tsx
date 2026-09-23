import { Button } from '@/components/ui/button'
import { LoadingRows } from '@/components/feedback/LoadingRows'
import { ErrorState } from '@/components/feedback/ErrorState'
import { EmptyState } from '@/components/feedback/EmptyState'
import { PageHeader } from '@/components/layout/PageHeader'
import { StatusChip } from '@/components/feedback/StatusChip'
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
        <div className="space-y-3">
          <div className="overflow-hidden rounded-lg border border-border/70 bg-card shadow-xs">
            <div className="overflow-x-auto">
              <table className="w-full text-sm" data-tabular="true">
                <thead className="bg-muted/40">
                  <tr className="border-b border-border/70">
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
                <tbody className="divide-y divide-border/70">
                  {data.content.map((batch) => {
                    const pct = batch.total_rows > 0
                      ? Math.round((batch.processed_rows / batch.total_rows) * 100)
                      : batch.status === 'COMPLETED' ? 100 : 0
                    return (
                      <tr key={batch.id} className="hover:bg-muted/30 transition-colors">
                        <td className="px-3 py-2.5 max-w-[280px]">
                          <Link
                            to={`/imports/${batch.id}`}
                            className="font-medium text-foreground hover:text-primary transition-colors block truncate"
                          >
                            {batch.file_name}
                          </Link>
                          <p className="text-xs text-muted-foreground uppercase">{batch.file_type}</p>
                        </td>
                        <td className="px-3 py-2.5">
                          <span className="inline-flex items-center rounded-md bg-muted px-1.5 py-0.5 text-xs font-medium">
                            {batch.source}
                          </span>
                        </td>
                        <td className="px-3 py-2.5 min-w-[160px]">
                          <StatusChip kind="batch" value={batch.status} />
                          {batch.status === 'PROCESSING' && (
                            <div className="mt-1.5 h-1 w-32 overflow-hidden rounded bg-muted">
                              <div
                                className="h-full rounded bg-primary transition-all duration-300"
                                style={{ width: `${pct}%` }}
                              />
                            </div>
                          )}
                        </td>
                        <td className="px-3 py-2.5 text-right tabular-nums">{batch.total_rows.toLocaleString()}</td>
                        <td className="px-3 py-2.5 text-right tabular-nums text-emerald-600 dark:text-emerald-400 font-medium">
                          {batch.created_rows.toLocaleString()}
                        </td>
                        <td className="px-3 py-2.5 text-right tabular-nums text-amber-700 dark:text-amber-400">
                          {batch.duplicate_rows.toLocaleString()}
                        </td>
                        <td className="px-3 py-2.5 text-right tabular-nums text-red-600 dark:text-red-400">
                          {batch.rejected_rows.toLocaleString()}
                        </td>
                        <td className="px-3 py-2.5 whitespace-nowrap text-muted-foreground">
                          {new Date(batch.created_at).toLocaleString(undefined, {
                            year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
                          })}
                        </td>
                        <td className="px-3 py-2.5 text-right pr-4">
                          <Button variant="ghost" size="sm" asChild>
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

          {data.total_pages > 1 && (
            <div className="flex items-center justify-between text-sm">
              <p className="text-muted-foreground tabular-nums">
                Page {data.page + 1} of {data.total_pages} · {data.total_elements.toLocaleString()} total
              </p>
              <div className="flex gap-2">
                <Button variant="outline" size="sm" disabled={data.page === 0} onClick={() => setPage((p) => p - 1)}>Previous</Button>
                <Button variant="outline" size="sm" disabled={data.page >= data.total_pages - 1} onClick={() => setPage((p) => p + 1)}>Next</Button>
              </div>
            </div>
          )}
        </div>
      ) : null}
    </div>
  )
}

function Th({ children, className }: { children: React.ReactNode; className?: string }) {
  return (
    <th scope="col" className={`px-3 py-2.5 text-left text-xs font-semibold uppercase tracking-wide text-muted-foreground ${className ?? ''}`}>
      {children}
    </th>
  )
}

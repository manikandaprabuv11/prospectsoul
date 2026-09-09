import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Plus } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'
import { useImportBatches } from '../hooks'

function statusBadge(status: string) {
  switch (status) {
    case 'COMPLETED': return <Badge variant="success">Completed</Badge>
    case 'PROCESSING': return <Badge variant="warning">Processing</Badge>
    case 'FAILED': return <Badge variant="destructive">Failed</Badge>
    case 'MAPPING': return <Badge variant="secondary">Mapping</Badge>
    case 'PREVIEWING': return <Badge variant="secondary">Previewing</Badge>
    default: return <Badge variant="outline">{status}</Badge>
  }
}

export function ImportListPage() {
  const [page, setPage] = useState(0)
  const { data, isLoading, isError, error } = useImportBatches(page)

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Imports</h1>
          <p className="text-sm text-muted-foreground">Upload and manage data imports</p>
        </div>
        <Button asChild>
          <Link to="/imports/new"><Plus className="size-4" /> New Import</Link>
        </Button>
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-16 w-full" />
          ))}
        </div>
      ) : isError ? (
        <div className="rounded-lg border border-destructive/50 p-6 text-center text-sm text-destructive">
          {error instanceof Error ? error.message : 'Failed to load imports'}
        </div>
      ) : data && data.content.length === 0 ? (
        <div className="rounded-lg border p-12 text-center">
          <p className="text-muted-foreground">No imports yet.</p>
          <Button className="mt-4" asChild>
            <Link to="/imports/new">Upload your first file</Link>
          </Button>
        </div>
      ) : data ? (
        <>
          <div className="overflow-x-auto rounded-lg border">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b bg-muted/50">
                  <th className="px-3 py-2.5 text-left font-medium text-muted-foreground">File</th>
                  <th className="px-3 py-2.5 text-left font-medium text-muted-foreground">Type</th>
                  <th className="px-3 py-2.5 text-left font-medium text-muted-foreground">Source</th>
                  <th className="px-3 py-2.5 text-left font-medium text-muted-foreground">Status</th>
                  <th className="px-3 py-2.5 text-left font-medium text-muted-foreground">Rows</th>
                  <th className="px-3 py-2.5 text-left font-medium text-muted-foreground">Created / Dup / Rejected</th>
                  <th className="px-3 py-2.5 text-left font-medium text-muted-foreground">Date</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((batch) => (
                  <tr key={batch.id} className="border-b last:border-0 hover:bg-muted/30">
                    <td className="px-3 py-2.5 font-medium">
                      <Link to={`/imports/${batch.id}`} className="text-primary hover:underline">
                        {batch.file_name}
                      </Link>
                    </td>
                    <td className="px-3 py-2.5">{batch.file_type}</td>
                    <td className="px-3 py-2.5">{batch.source}</td>
                    <td className="px-3 py-2.5">{statusBadge(batch.status)}</td>
                    <td className="px-3 py-2.5">{batch.total_rows}</td>
                    <td className="px-3 py-2.5">
                      <span className="text-emerald-600">{batch.created_rows}</span>
                      {' / '}
                      <span className="text-amber-600">{batch.duplicate_rows}</span>
                      {' / '}
                      <span className="text-red-600">{batch.rejected_rows}</span>
                    </td>
                    <td className="px-3 py-2.5 text-muted-foreground">
                      {new Date(batch.created_at).toLocaleDateString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {data.total_pages > 1 && (
            <div className="flex items-center justify-between">
              <p className="text-sm text-muted-foreground">
                Page {data.page + 1} of {data.total_pages}
              </p>
              <div className="flex gap-2">
                <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)}>
                  Previous
                </Button>
                <Button variant="outline" size="sm" disabled={page >= data.total_pages - 1} onClick={() => setPage(page + 1)}>
                  Next
                </Button>
              </div>
            </div>
          )}
        </>
      ) : null}
    </div>
  )
}

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { ArrowLeft } from 'lucide-react'
import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { useImportBatch, useImportRows } from '../hooks'

function rowStatusBadge(status: string) {
  switch (status) {
    case 'CREATED': return <Badge variant="success">Created</Badge>
    case 'DUPLICATE': return <Badge variant="warning">Duplicate</Badge>
    case 'REJECTED': return <Badge variant="destructive">Rejected</Badge>
    case 'FAILED': return <Badge variant="destructive">Failed</Badge>
    default: return <Badge variant="secondary">{status}</Badge>
  }
}

export function ImportDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { data: batch, isLoading } = useImportBatch(id)
  const [rowPage, setRowPage] = useState(0)
  const isLive = batch?.status === 'PROCESSING'
  const { data: rows } = useImportRows(id, rowPage, 25, isLive)

  if (isLoading) {
    return <div className="space-y-4"><Skeleton className="h-8 w-64" /><Skeleton className="h-48 w-full" /></div>
  }
  if (!batch) {
    return <div className="p-6 text-center text-muted-foreground">Import batch not found</div>
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon" asChild>
          <Link to="/imports"><ArrowLeft className="size-4" /></Link>
        </Button>
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">{batch.file_name}</h1>
          <p className="text-sm text-muted-foreground">
            {batch.file_type} · {batch.source} · {batch.status}
          </p>
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-4">
        <Card>
          <CardHeader className="pb-2"><CardTitle className="text-sm font-medium text-muted-foreground">Total Rows</CardTitle></CardHeader>
          <CardContent><p className="text-2xl font-bold">{batch.total_rows}</p></CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2"><CardTitle className="text-sm font-medium text-emerald-600">Created</CardTitle></CardHeader>
          <CardContent><p className="text-2xl font-bold text-emerald-600">{batch.created_rows}</p></CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2"><CardTitle className="text-sm font-medium text-amber-600">Duplicates</CardTitle></CardHeader>
          <CardContent><p className="text-2xl font-bold text-amber-600">{batch.duplicate_rows}</p></CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2"><CardTitle className="text-sm font-medium text-red-600">Rejected</CardTitle></CardHeader>
          <CardContent><p className="text-2xl font-bold text-red-600">{batch.rejected_rows}</p></CardContent>
        </Card>
      </div>

      {batch.status === 'PROCESSING' && (
        <div className="rounded-lg border bg-card p-4 space-y-2">
          <div className="flex items-center justify-between text-sm">
            <div>
              <span className="font-medium">Processing…</span>{' '}
              <span className="text-muted-foreground">
                {batch.processed_rows} of {batch.total_rows} rows
              </span>
            </div>
            <div className="text-xs text-muted-foreground">
              Runs on the server — safe to close this tab.
            </div>
          </div>
          <div className="h-2 w-full rounded bg-muted overflow-hidden">
            <div
              className="h-full bg-primary transition-all duration-300"
              style={{
                width:
                  batch.total_rows > 0
                    ? `${Math.min(100, Math.round((batch.processed_rows / batch.total_rows) * 100))}%`
                    : '0%',
              }}
            />
          </div>
        </div>
      )}

      {batch.error_message && (
        <div className="rounded-lg border border-destructive/50 bg-destructive/10 p-4 text-sm text-destructive">
          {batch.error_message}
        </div>
      )}

      {rows && (
        <Card>
          <CardHeader><CardTitle>Import Rows</CardTitle></CardHeader>
          <CardContent>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b bg-muted/50">
                    <th className="px-3 py-2 text-left font-medium text-muted-foreground">#</th>
                    <th className="px-3 py-2 text-left font-medium text-muted-foreground">Status</th>
                    <th className="px-3 py-2 text-left font-medium text-muted-foreground">Raw Data</th>
                    <th className="px-3 py-2 text-left font-medium text-muted-foreground">Error</th>
                    <th className="px-3 py-2 text-left font-medium text-muted-foreground">Company</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.content.map((row) => (
                    <tr key={row.id} className="border-b last:border-0">
                      <td className="px-3 py-2">{row.row_number}</td>
                      <td className="px-3 py-2">{rowStatusBadge(row.status)}</td>
                      <td className="max-w-md truncate px-3 py-2 font-mono text-xs">
                        {JSON.stringify(row.raw_data).slice(0, 100)}...
                      </td>
                      <td className="px-3 py-2 text-destructive">{row.error_message ?? '—'}</td>
                      <td className="px-3 py-2">
                        {row.company_id ? (
                          <Link to={`/companies/${row.company_id}`} className="text-primary hover:underline">
                            View
                          </Link>
                        ) : row.duplicate_of_company_id ? (
                          <Link to={`/companies/${row.duplicate_of_company_id}`} className="text-amber-600 hover:underline">
                            Duplicate
                          </Link>
                        ) : '—'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {rows.total_pages > 1 && (
              <div className="mt-4 flex items-center justify-between">
                <p className="text-sm text-muted-foreground">Page {rows.page + 1} of {rows.total_pages}</p>
                <div className="flex gap-2">
                  <Button variant="outline" size="sm" disabled={rowPage === 0} onClick={() => setRowPage(rowPage - 1)}>Previous</Button>
                  <Button variant="outline" size="sm" disabled={rowPage >= rows.total_pages - 1} onClick={() => setRowPage(rowPage + 1)}>Next</Button>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  )
}

import { useState } from 'react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { PageHeader } from '@/components/layout/PageHeader'
import { LoadingRows } from '@/components/feedback/LoadingRows'
import { ErrorState } from '@/components/feedback/ErrorState'
import { Button } from '@/components/ui/button'
import { Pagination } from '@/shared/components/Pagination'
import { ChevronDown, ChevronUp } from 'lucide-react'
import { usePermissions } from '@/auth/usePermissions'
import { useAllEnrichmentJobs } from '../hooks'
import { BatchEnrichPanel } from '../components/BatchEnrichPanel'

const STATUS_COLORS: Record<string, string> = {
  SUCCESS: 'bg-green-100 text-green-800',
  PARTIAL: 'bg-yellow-100 text-yellow-800',
  RUNNING: 'bg-blue-100 text-blue-800',
  QUEUED: 'bg-gray-100 text-gray-800',
  FAILED: 'bg-red-100 text-red-800',
}

/**
 * `/enrichment/jobs` — mirrors the Verify workspace layout: a header with a
 * toggle for the selection/run panel, then the full job history below.
 */
export function EnrichmentJobsPage() {
  const { canMutate } = usePermissions()
  const [page, setPage] = useState(0)
  const { data, isLoading, isError, error } = useAllEnrichmentJobs(page)

  const [selectionOpen, setSelectionOpen] = useState(canMutate)

  const jobs = data?.content ?? []
  const totalPages = data?.totalPages ?? 0

  return (
    <div className="space-y-6">
      <PageHeader
        title="Enrichment Jobs"
        description="Run enrichment against Google Places, website and phone providers, and review the results across all companies."
        actions={
          canMutate ? (
            <Button
              variant={selectionOpen ? 'outline' : 'default'}
              onClick={() => setSelectionOpen((open) => !open)}
              aria-expanded={selectionOpen}
              aria-controls="run-enrichment"
            >
              {selectionOpen ? (<><ChevronUp /> Hide selection</>) : (<><ChevronDown /> Run enrichment</>)}
            </Button>
          ) : null
        }
      />

      {selectionOpen && (
        <div id="run-enrichment">
          <BatchEnrichPanel canRun={canMutate} />
        </div>
      )}

      {isLoading ? (
        <LoadingRows count={5} height="h-12" />
      ) : isError ? (
        <ErrorState message={error instanceof Error ? error.message : 'Failed to load enrichment jobs'} />
      ) : (
        <Card>
          <CardHeader className="border-b-0 pb-2">
            <CardTitle className="flex items-center gap-2 text-sm">
              Jobs
              <Badge variant="secondary">{data?.totalElements ?? 0}</Badge>
            </CardTitle>
          </CardHeader>
          <CardContent className="pt-2 space-y-4">
            {jobs.length === 0 ? (
              <div className="text-sm text-muted-foreground py-8 text-center">No enrichment jobs yet.</div>
            ) : (
              <div className="space-y-2">
                {jobs.map((job) => (
                  <div key={job.id} className="flex items-center justify-between rounded-md border p-3 text-sm">
                    <div className="flex items-center gap-2">
                      <Badge variant="outline" className="text-xs">{job.provider_key}</Badge>
                      <Badge className={`text-xs ${STATUS_COLORS[job.status] ?? 'bg-gray-100 text-gray-800'}`}>
                        {job.status}
                      </Badge>
                      <span className="text-xs text-muted-foreground font-mono">{job.company_id.slice(0, 8)}…</span>
                    </div>
                    <div className="flex items-center gap-3 text-xs text-muted-foreground">
                      {job.facts_added > 0 && <span>+{job.facts_added} facts</span>}
                      {job.facts_updated > 0 && <span>~{job.facts_updated} updated</span>}
                      {job.candidates_added > 0 && <span>{job.candidates_added} candidates</span>}
                      {job.cost_usd > 0 && <span>${job.cost_usd.toFixed(3)}</span>}
                      {job.error_code && <span className="text-red-600">{job.error_code}</span>}
                      <span>{job.started_at ? new Date(job.started_at).toLocaleString() : '—'}</span>
                    </div>
                  </div>
                ))}
              </div>
            )}

            <Pagination
              page={page}
              totalPages={totalPages}
              totalElements={data?.totalElements}
              itemLabel="jobs"
              onPageChange={setPage}
            />
          </CardContent>
        </Card>
      )}
    </div>
  )
}

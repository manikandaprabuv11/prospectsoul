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

const STATUS_STYLES: Record<string, string> = {
  SUCCESS: 'bg-accent-emerald/10 text-accent-emerald border border-accent-emerald/20',
  PARTIAL: 'bg-accent-amber/10 text-accent-amber border border-accent-amber/20',
  RUNNING: 'bg-accent-sky/10 text-accent-sky border border-accent-sky/20',
  QUEUED: 'bg-surface-1 text-muted-foreground border border-border',
  FAILED: 'bg-accent-rose/10 text-accent-rose border border-accent-rose/20',
}

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
        <div id="run-enrichment" className="animate-slide-up">
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
              <Badge variant="secondary" className="tabular-nums">{data?.totalElements ?? 0}</Badge>
            </CardTitle>
          </CardHeader>
          <CardContent className="pt-2 space-y-4">
            {jobs.length === 0 ? (
              <div className="rounded-xl border border-border bg-surface-1/50 py-12 text-center">
                <p className="font-semibold">No enrichment jobs yet</p>
                <p className="text-sm text-muted-foreground mt-1">Run an enrichment from the panel above to get started.</p>
              </div>
            ) : (
              <div className="space-y-2">
                {jobs.map((job) => (
                  <div key={job.id} className="flex items-center justify-between rounded-xl border border-border bg-card p-4 text-sm hover:shadow-card transition-shadow duration-200">
                    <div className="flex items-center gap-2.5">
                      <Badge variant="outline" className="text-[11px] font-semibold">{job.provider_key}</Badge>
                      <span className={`inline-flex items-center rounded-md px-2 py-0.5 text-[11px] font-semibold ${STATUS_STYLES[job.status] ?? 'bg-surface-1 text-muted-foreground border border-border'}`}>
                        {job.status}
                      </span>
                      <span className="text-xs text-muted-foreground font-mono">{job.company_id.slice(0, 8)}...</span>
                    </div>
                    <div className="flex items-center gap-4 text-xs text-muted-foreground">
                      {job.facts_added > 0 && <span className="font-medium text-accent-emerald">+{job.facts_added} facts</span>}
                      {job.facts_updated > 0 && <span className="font-medium text-accent-sky">~{job.facts_updated} updated</span>}
                      {job.candidates_added > 0 && <span className="font-medium">{job.candidates_added} candidates</span>}
                      {job.cost_usd > 0 && <span className="tabular-nums font-medium">${job.cost_usd.toFixed(3)}</span>}
                      {job.error_code && <span className="font-medium text-accent-rose">{job.error_code}</span>}
                      <span className="text-[12px]">{job.started_at ? new Date(job.started_at).toLocaleString() : '—'}</span>
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

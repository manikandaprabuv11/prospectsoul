import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { useEnrichmentJobs } from '../hooks'

interface EnrichmentJobsPanelProps {
  companyId: string
}

const STATUS_STYLES: Record<string, string> = {
  SUCCESS: 'bg-accent-emerald/10 text-accent-emerald border border-accent-emerald/20',
  PARTIAL: 'bg-accent-amber/10 text-accent-amber border border-accent-amber/20',
  RUNNING: 'bg-accent-sky/10 text-accent-sky border border-accent-sky/20',
  QUEUED: 'bg-surface-1 text-muted-foreground border border-border',
  FAILED: 'bg-accent-rose/10 text-accent-rose border border-accent-rose/20',
}

export function EnrichmentJobsPanel({ companyId }: EnrichmentJobsPanelProps) {
  const { data, isLoading } = useEnrichmentJobs(companyId)

  const jobs = data?.content ?? []

  if (isLoading) {
    return (
      <Card>
        <CardHeader className="border-b-0 pb-2">
          <CardTitle className="text-sm">Enrichment History</CardTitle>
        </CardHeader>
        <CardContent className="pt-2">
          <div className="space-y-2">
            {[0, 1].map((i) => <div key={i} className="h-10 animate-pulse rounded-xl bg-surface-1" />)}
          </div>
        </CardContent>
      </Card>
    )
  }

  if (jobs.length === 0) {
    return (
      <Card>
        <CardHeader className="border-b-0 pb-2">
          <CardTitle className="text-sm">Enrichment History</CardTitle>
        </CardHeader>
        <CardContent className="pt-2">
          <div className="rounded-xl border border-dashed border-border bg-surface-1/50 py-6 text-center text-sm text-muted-foreground">
            No enrichment jobs yet. Use the Enrich button to start.
          </div>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card>
      <CardHeader className="border-b-0 pb-2">
        <CardTitle className="text-sm">Enrichment History</CardTitle>
      </CardHeader>
      <CardContent className="pt-2">
        <div className="space-y-1.5">
          {jobs.map((job) => (
            <div key={job.id} className="flex items-center justify-between rounded-xl border border-border p-2.5 text-sm transition-shadow hover:shadow-card">
              <div className="flex items-center gap-2">
                <Badge variant="outline">{job.provider_key}</Badge>
                <span className={`inline-flex items-center rounded-md px-2 py-0.5 text-[11px] font-semibold ${STATUS_STYLES[job.status] ?? STATUS_STYLES.QUEUED}`}>
                  {job.status}
                </span>
              </div>
              <div className="flex items-center gap-3 text-xs text-muted-foreground tabular-nums">
                {job.facts_added > 0 && <span className="text-accent-emerald font-medium">+{job.facts_added} facts</span>}
                {job.facts_updated > 0 && <span className="text-accent-sky font-medium">~{job.facts_updated} updated</span>}
                {job.candidates_added > 0 && <span className="text-accent-violet font-medium">{job.candidates_added} candidates</span>}
                {job.cost_usd > 0 && <span>${job.cost_usd.toFixed(3)}</span>}
                <span>{job.started_at ? new Date(job.started_at).toLocaleString() : '—'}</span>
              </div>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  )
}

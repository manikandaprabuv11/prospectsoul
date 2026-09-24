import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { useEnrichmentJobs } from '../hooks'

interface EnrichmentJobsPanelProps {
  companyId: string
}

const STATUS_COLORS: Record<string, string> = {
  SUCCESS: 'bg-green-100 text-green-800',
  PARTIAL: 'bg-yellow-100 text-yellow-800',
  RUNNING: 'bg-blue-100 text-blue-800',
  QUEUED: 'bg-gray-100 text-gray-800',
  FAILED: 'bg-red-100 text-red-800',
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
          <div className="text-sm text-muted-foreground">Loading…</div>
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
          <div className="text-sm text-muted-foreground">No enrichment jobs yet. Use the Enrich button to start.</div>
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
        <div className="space-y-2">
          {jobs.map((job) => (
            <div key={job.id} className="flex items-center justify-between rounded-md border p-2 text-sm">
              <div className="flex items-center gap-2">
                <Badge variant="outline" className="text-xs">{job.provider_key}</Badge>
                <Badge className={`text-xs ${STATUS_COLORS[job.status] ?? 'bg-gray-100 text-gray-800'}`}>
                  {job.status}
                </Badge>
              </div>
              <div className="flex items-center gap-3 text-xs text-muted-foreground">
                {job.facts_added > 0 && <span>+{job.facts_added} facts</span>}
                {job.facts_updated > 0 && <span>~{job.facts_updated} updated</span>}
                {job.candidates_added > 0 && <span>{job.candidates_added} candidates</span>}
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

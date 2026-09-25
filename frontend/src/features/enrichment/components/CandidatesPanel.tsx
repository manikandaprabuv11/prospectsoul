import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Check, X, Loader2 } from 'lucide-react'
import { useEnrichmentCandidates, useResolveCandidate } from '../hooks'

interface CandidatesPanelProps {
  companyId: string
}

export function CandidatesPanel({ companyId }: CandidatesPanelProps) {
  const { data, isLoading } = useEnrichmentCandidates(companyId, 'PENDING')
  const resolveMutation = useResolveCandidate(companyId)

  const candidates = data?.content ?? []

  if (isLoading) {
    return (
      <Card>
        <CardHeader className="border-b-0 pb-2">
          <CardTitle className="text-sm">Enrichment Candidates</CardTitle>
        </CardHeader>
        <CardContent className="pt-2">
          <div className="space-y-2">
            {[0, 1].map((i) => <div key={i} className="h-16 animate-pulse rounded-xl bg-surface-1" />)}
          </div>
        </CardContent>
      </Card>
    )
  }

  if (candidates.length === 0) return null

  return (
    <Card>
      <CardHeader className="border-b-0 pb-2">
        <CardTitle className="flex items-center gap-2 text-sm">
          Enrichment Candidates
          <Badge variant="secondary">{candidates.length}</Badge>
        </CardTitle>
      </CardHeader>
      <CardContent className="pt-2">
        <div className="space-y-2">
          {candidates.map((c) => (
            <div key={c.id} className="flex items-start justify-between gap-4 rounded-xl border border-border bg-card p-3 transition-shadow hover:shadow-card">
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2 text-sm">
                  <Badge variant="outline">{c.provider_key}</Badge>
                  <span className="font-semibold">{formatFieldName(c.field_name)}</span>
                  <Badge variant="secondary">{c.candidate_type}</Badge>
                </div>
                <div className="mt-1.5 text-sm">
                  <span className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Proposed: </span>
                  <span className="rounded bg-surface-1 px-1.5 py-0.5 font-mono text-xs">{c.proposed_value}</span>
                </div>
                {c.current_value && (
                  <div className="mt-1 text-sm">
                    <span className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Current: </span>
                    <span className="rounded bg-surface-1 px-1.5 py-0.5 font-mono text-xs">{c.current_value}</span>
                  </div>
                )}
              </div>
              <div className="flex items-center gap-1.5 shrink-0">
                <Button
                  size="sm"
                  variant="outline"
                  className="h-7 text-accent-emerald hover:text-accent-emerald hover:bg-accent-emerald/10 hover:border-accent-emerald/30"
                  disabled={resolveMutation.isPending}
                  onClick={() => resolveMutation.mutate({ candidateId: c.id, data: { action: 'ACCEPTED' } })}
                >
                  {resolveMutation.isPending ? <Loader2 className="size-3 animate-spin" /> : <Check className="size-3" />}
                  Accept
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  className="h-7 text-accent-rose hover:text-accent-rose hover:bg-accent-rose/10 hover:border-accent-rose/30"
                  disabled={resolveMutation.isPending}
                  onClick={() => resolveMutation.mutate({ candidateId: c.id, data: { action: 'REJECTED' } })}
                >
                  {resolveMutation.isPending ? <Loader2 className="size-3 animate-spin" /> : <X className="size-3" />}
                  Reject
                </Button>
              </div>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  )
}

function formatFieldName(field: string): string {
  return field.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase())
}

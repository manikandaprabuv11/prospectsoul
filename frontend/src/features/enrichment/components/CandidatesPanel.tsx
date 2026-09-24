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
          <div className="text-sm text-muted-foreground">Loading…</div>
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
        <div className="space-y-3">
          {candidates.map((c) => (
            <div key={c.id} className="flex items-start justify-between gap-4 rounded-md border p-3">
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2 text-sm">
                  <Badge variant="outline" className="text-xs">{c.provider_key}</Badge>
                  <span className="font-medium">{formatFieldName(c.field_name)}</span>
                  <Badge variant="secondary" className="text-xs">{c.candidate_type}</Badge>
                </div>
                <div className="mt-1 text-sm">
                  <span className="text-muted-foreground">Proposed: </span>
                  <span className="font-mono text-xs">{c.proposed_value}</span>
                </div>
                {c.current_value && (
                  <div className="text-sm">
                    <span className="text-muted-foreground">Current: </span>
                    <span className="font-mono text-xs">{c.current_value}</span>
                  </div>
                )}
              </div>
              <div className="flex items-center gap-1">
                <Button
                  size="sm"
                  variant="outline"
                  className="h-7 text-green-600 hover:text-green-700"
                  disabled={resolveMutation.isPending}
                  onClick={() => resolveMutation.mutate({ candidateId: c.id, data: { action: 'ACCEPTED' } })}
                >
                  {resolveMutation.isPending ? <Loader2 className="size-3 animate-spin" /> : <Check className="size-3" />}
                  Accept
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  className="h-7 text-red-600 hover:text-red-700"
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

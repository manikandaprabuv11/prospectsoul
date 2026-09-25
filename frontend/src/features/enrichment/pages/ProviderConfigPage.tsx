import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { PageHeader } from '@/components/layout/PageHeader'
import { LoadingRows } from '@/components/feedback/LoadingRows'
import { ErrorState } from '@/components/feedback/ErrorState'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { useProviderConfigs } from '../hooks'
import { enrichmentApi } from '../api'
import { queryClient } from '@/lib/query-client'
import type { ProviderConfig } from '../types'

export function ProviderConfigPage() {
  const { data: configs, isLoading, isError, error } = useProviderConfigs()

  if (isLoading) return <LoadingRows count={3} height="h-24" />
  if (isError) return <ErrorState message={error instanceof Error ? error.message : 'Failed to load provider configs'} />

  return (
    <div className="space-y-6">
      <PageHeader title="Enrichment Providers" description="Admin configuration for enrichment providers" />

      <div className="grid gap-4 md:grid-cols-3">
        {(configs ?? []).map((config) => (
          <ProviderCard key={config.provider_key} config={config} />
        ))}
      </div>
    </div>
  )
}

function ProviderCard({ config }: { config: ProviderConfig }) {
  async function toggleEnabled() {
    await enrichmentApi.updateProviderConfig(config.provider_key, { enabled: !config.enabled })
    queryClient.invalidateQueries({ queryKey: ['provider-configs'] })
  }

  return (
    <Card className="group hover:shadow-card-hover transition-shadow duration-200">
      <CardHeader className="border-b-0 pb-2">
        <CardTitle className="flex items-center justify-between text-sm">
          <span className="font-bold">{config.provider_key}</span>
          <Badge variant={config.enabled ? 'default' : 'secondary'}>
            {config.enabled ? 'Enabled' : 'Disabled'}
          </Badge>
        </CardTitle>
      </CardHeader>
      <CardContent className="pt-2 space-y-4">
        <dl className="grid grid-cols-2 gap-x-4 gap-y-2.5 text-sm">
          <dt className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Max retries</dt>
          <dd className="font-medium">{config.max_retries}</dd>
          <dt className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Idempotency</dt>
          <dd className="font-medium">{config.idempotency_window_hours}h</dd>
          <dt className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Cost/call</dt>
          <dd className="font-medium tabular-nums">${config.cost_per_call_usd}</dd>
          <dt className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Rate (sec)</dt>
          <dd className="font-medium tabular-nums">{config.rate_limit_per_sec ?? <span className="text-muted-foreground/60">—</span>}</dd>
          <dt className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Rate (day)</dt>
          <dd className="font-medium tabular-nums">{config.rate_limit_per_day ?? <span className="text-muted-foreground/60">—</span>}</dd>
          <dt className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Timeout</dt>
          <dd className="font-medium tabular-nums">{config.timeout_ms}ms</dd>
        </dl>
        <Button variant="outline" size="sm" className="w-full" onClick={toggleEnabled}>
          {config.enabled ? 'Disable' : 'Enable'}
        </Button>
      </CardContent>
    </Card>
  )
}

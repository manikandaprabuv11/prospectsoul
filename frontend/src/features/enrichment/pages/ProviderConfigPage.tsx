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
    <Card>
      <CardHeader className="border-b-0 pb-2">
        <CardTitle className="flex items-center justify-between text-sm">
          <span>{config.provider_key}</span>
          <Badge variant={config.enabled ? 'default' : 'secondary'}>
            {config.enabled ? 'Enabled' : 'Disabled'}
          </Badge>
        </CardTitle>
      </CardHeader>
      <CardContent className="pt-2 space-y-3">
        <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
          <dt className="text-muted-foreground">Max retries</dt>
          <dd>{config.max_retries}</dd>
          <dt className="text-muted-foreground">Idempotency</dt>
          <dd>{config.idempotency_window_hours}h</dd>
          <dt className="text-muted-foreground">Cost/call</dt>
          <dd>${config.cost_per_call_usd}</dd>
          <dt className="text-muted-foreground">Rate (sec)</dt>
          <dd>{config.rate_limit_per_sec ?? '—'}</dd>
          <dt className="text-muted-foreground">Rate (day)</dt>
          <dd>{config.rate_limit_per_day ?? '—'}</dd>
          <dt className="text-muted-foreground">Timeout</dt>
          <dd>{config.timeout_ms}ms</dd>
        </dl>
        <Button variant="outline" size="sm" className="w-full" onClick={toggleEnabled}>
          {config.enabled ? 'Disable' : 'Enable'}
        </Button>
      </CardContent>
    </Card>
  )
}

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Pagination } from '@/shared/components/Pagination'
import { Skeleton } from '@/components/ui/skeleton'
import { CheckCircle2, Loader2, Sparkles, XCircle } from 'lucide-react'
import { useState } from 'react'
import { useCompanies } from '@/features/company/hooks'
import { companyApi } from '@/features/company/api'
import { CompanyFilters } from '@/features/company/components/CompanyFilters'
import type { Company, CompanyFilters as Filters } from '@/features/company/types'
import { queryClient } from '@/lib/query-client'
import { enrichmentApi } from '../api'

const PAGE_SIZE = 25

const DEFAULT_FILTERS: Filters = {
  page: 0,
  size: PAGE_SIZE,
  sort: 'createdAt',
  sort_dir: 'desc',
}

const PROVIDERS = [
  { key: 'GOOGLE_PLACES', label: 'Google Places' },
  { key: 'WEBSITE', label: 'Website (Firecrawl)' },
  { key: 'PHONE', label: 'Phone' },
] as const

type RunStatus = 'queued' | 'running' | 'success' | 'partial' | 'failed'

interface RunState {
  status: RunStatus
  detail?: string
}

const RUN_STATUS_LABEL: Record<RunStatus, string> = {
  queued: 'Queued',
  running: 'Running…',
  success: 'Success',
  partial: 'Partial',
  failed: 'Failed',
}

const RUN_STATUS_STYLES: Record<RunStatus, string> = {
  queued: 'bg-surface-1 text-muted-foreground border border-border',
  running: 'bg-accent-sky/10 text-accent-sky border border-accent-sky/20',
  success: 'bg-accent-emerald/10 text-accent-emerald border border-accent-emerald/20',
  partial: 'bg-accent-amber/10 text-accent-amber border border-accent-amber/20',
  failed: 'bg-accent-rose/10 text-accent-rose border border-accent-rose/20',
}

interface Props {
  canRun: boolean
}

export function BatchEnrichPanel({ canRun }: Props) {
  const [filters, setFilters] = useState<Filters>(DEFAULT_FILTERS)
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [providers, setProviders] = useState<Set<string>>(new Set(PROVIDERS.map((p) => p.key)))
  const [confirming, setConfirming] = useState(false)
  const [running, setRunning] = useState(false)
  const [runStates, setRunStates] = useState<Map<string, RunState>>(new Map())
  const [selectingAll, setSelectingAll] = useState(false)
  const [allFilterSelected, setAllFilterSelected] = useState(false)

  const companies = useCompanies(filters)

  const rows = companies.data?.content ?? []
  const totalElements = companies.data?.total_elements ?? 0
  const totalPages = companies.data?.total_pages ?? 1
  const allOnPageSelected = rows.length > 0 && rows.every((row) => selected.has(row.id))
  const someOnPageSelected = rows.some((row) => selected.has(row.id))
  const showSelectAllBanner = allOnPageSelected && totalPages > 1 && !allFilterSelected

  function handleFiltersChange(next: Filters) {
    setFilters(next)
    setAllFilterSelected(false)
  }

  function toggleRow(id: string, checked: boolean) {
    setAllFilterSelected(false)
    setSelected((current) => {
      const next = new Set(current)
      if (checked) next.add(id)
      else next.delete(id)
      return next
    })
  }

  function toggleCurrentPage(checked: boolean) {
    if (!checked) setAllFilterSelected(false)
    setSelected((current) => {
      const next = new Set(current)
      for (const row of rows) {
        if (checked) next.add(row.id)
        else next.delete(row.id)
      }
      return next
    })
  }

  async function selectAllMatching() {
    setSelectingAll(true)
    try {
      const result = await companyApi.listIds(filters)
      setSelected(new Set(result.ids))
      setAllFilterSelected(true)
    } finally {
      setSelectingAll(false)
    }
  }

  function clearSelection() {
    setSelected(new Set())
    setAllFilterSelected(false)
  }

  function toggleProvider(key: string, checked: boolean) {
    setProviders((current) => {
      const next = new Set(current)
      if (checked) next.add(key)
      else next.delete(key)
      return next
    })
  }

  async function runBatch() {
    const ids = [...selected]
    const providerKeys = [...providers]
    setRunning(true)
    setConfirming(false)
    const initial = new Map<string, RunState>(ids.map((id) => [id, { status: 'queued' as const }]))
    setRunStates(initial)

    const CONCURRENCY = 3
    let cursor = 0

    async function worker() {
      while (cursor < ids.length) {
        const id = ids[cursor++]
        if (!id) continue
        setRunStates((prev) => new Map(prev).set(id, { status: 'running' }))
        try {
          const jobs = await enrichmentApi.enrichCompany(id, { provider_keys: providerKeys, force: false })
          const failed = jobs.filter((j) => j.status === 'FAILED').length
          const status: RunStatus = failed === 0 ? 'success' : failed === jobs.length ? 'failed' : 'partial'
          setRunStates((prev) =>
            new Map(prev).set(id, {
              status,
              detail: `${jobs.length - failed}/${jobs.length} provider${jobs.length === 1 ? '' : 's'} succeeded`,
            }),
          )
        } catch (err) {
          setRunStates((prev) =>
            new Map(prev).set(id, { status: 'failed', detail: err instanceof Error ? err.message : 'Request failed' }),
          )
        } finally {
          queryClient.invalidateQueries({ queryKey: ['enrichment-jobs', id] })
          queryClient.invalidateQueries({ queryKey: ['company', id] })
        }
      }
    }

    await Promise.all(Array.from({ length: Math.min(CONCURRENCY, ids.length) }, () => worker()))

    setRunning(false)
    queryClient.invalidateQueries({ queryKey: ['enrichment-jobs-all'] })
    queryClient.invalidateQueries({ queryKey: ['companies'] })
  }

  const runSummary = summarizeRuns(runStates)
  const hasRunResults = runStates.size > 0

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between gap-3 pb-3">
        <CardTitle>Run Enrichment</CardTitle>
        <Badge variant="secondary">{selected.size} selected</Badge>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="rounded-xl border border-border bg-card p-4 shadow-card">
          <CompanyFilters filters={filters} onChange={handleFiltersChange} />
        </div>

        <div className="flex flex-wrap items-center gap-4 rounded-xl border border-border bg-surface-1/50 p-3">
          <span className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Providers</span>
          {PROVIDERS.map((provider) => (
            <label key={provider.key} className="flex items-center gap-2 text-sm cursor-pointer">
              <Checkbox
                checked={providers.has(provider.key)}
                onCheckedChange={(checked) => toggleProvider(provider.key, checked === true)}
                disabled={running}
              />
              {provider.label}
            </label>
          ))}
        </div>

        {companies.isLoading ? (
          <div className="space-y-2">
            {Array.from({ length: 5 }).map((_, index) => (
              <Skeleton key={index} className="h-11 w-full" />
            ))}
          </div>
        ) : companies.isError ? (
          <div role="alert" className="rounded-xl border border-destructive/30 bg-destructive/5 p-6 text-center text-sm font-medium text-destructive">
            {companies.error instanceof Error ? companies.error.message : 'Could not load companies.'}
          </div>
        ) : rows.length === 0 ? (
          <div className="rounded-xl border border-dashed border-border bg-surface-1/50 py-12 text-center">
            <p className="font-semibold">No companies match these filters</p>
            <p className="mt-1 text-sm text-muted-foreground">Try adjusting the filters above.</p>
          </div>
        ) : (
          <>
            {showSelectAllBanner && (
              <div className="flex items-center justify-center gap-2 rounded-lg border border-primary/20 bg-primary/5 px-4 py-2.5 text-sm">
                <span>
                  All <strong>{rows.length}</strong> companies on this page are selected.
                </span>
                <Button
                  variant="link"
                  size="sm"
                  className="h-auto p-0 font-semibold"
                  onClick={selectAllMatching}
                  disabled={selectingAll}
                >
                  {selectingAll ? (
                    <><Loader2 className="size-3 animate-spin" /> Selecting…</>
                  ) : (
                    <>Select all {totalElements.toLocaleString()} companies matching this filter</>
                  )}
                </Button>
              </div>
            )}

            {allFilterSelected && (
              <div className="flex items-center justify-center gap-2 rounded-lg border border-accent-emerald/20 bg-accent-emerald/5 px-4 py-2.5 text-sm">
                <span>
                  All <strong>{selected.size.toLocaleString()}</strong> companies matching this filter are selected.
                </span>
                <Button variant="link" size="sm" className="h-auto p-0 font-semibold" onClick={clearSelection}>
                  Clear selection
                </Button>
              </div>
            )}

            <div className="overflow-x-auto rounded-xl border border-border shadow-card">
              <table className="w-full text-sm">
                <caption className="sr-only">Companies available for enrichment</caption>
                <thead>
                  <tr className="border-b border-border bg-surface-1">
                    <th scope="col" className="w-10 px-3 py-2.5 text-left">
                      <Checkbox
                        checked={allOnPageSelected ? true : someOnPageSelected ? 'indeterminate' : false}
                        onCheckedChange={(checked) => toggleCurrentPage(checked === true)}
                        disabled={!canRun || running || rows.length === 0}
                        aria-label="Select all companies on this page"
                      />
                    </th>
                    <th scope="col" className="px-3 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Company</th>
                    <th scope="col" className="px-3 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Location</th>
                    <th scope="col" className="px-3 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Industry</th>
                    <th scope="col" className="px-3 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Last enriched</th>
                    {hasRunResults && (
                      <th scope="col" className="px-3 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Result</th>
                    )}
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {rows.map((row) => {
                    const runState = runStates.get(row.id)
                    return (
                      <tr key={row.id} className="group/row hover:bg-surface-1/50 transition-colors">
                        <td className="px-3 py-2.5">
                          <Checkbox
                            checked={selected.has(row.id)}
                            onCheckedChange={(checked) => toggleRow(row.id, checked === true)}
                            disabled={!canRun || running}
                            aria-label={`Select ${row.canonical_name}`}
                          />
                        </td>
                        <td className="px-3 py-2.5 font-semibold">{row.canonical_name}</td>
                        <td className="px-3 py-2.5">{row.city ?? '—'}</td>
                        <td className="px-3 py-2.5">{row.industry ?? '—'}</td>
                        <td className="px-3 py-2.5 text-xs text-muted-foreground tabular-nums">{lastEnrichedSummary(row)}</td>
                        {hasRunResults && (
                          <td className="px-3 py-2.5">
                            {runState ? (
                              <div className="flex items-center gap-1.5">
                                {runState.status === 'running' && <Loader2 className="size-3 animate-spin text-accent-sky" />}
                                {runState.status === 'success' && <CheckCircle2 className="size-3 text-accent-emerald" />}
                                {runState.status === 'failed' && <XCircle className="size-3 text-accent-rose" />}
                                <span className={`inline-flex items-center rounded-md px-2 py-0.5 text-[11px] font-semibold ${RUN_STATUS_STYLES[runState.status]}`}>
                                  {RUN_STATUS_LABEL[runState.status]}
                                </span>
                                {runState.detail && (
                                  <span className="text-xs text-muted-foreground">{runState.detail}</span>
                                )}
                              </div>
                            ) : (
                              '—'
                            )}
                          </td>
                        )}
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>

            <div className="flex items-center gap-3">
              <p aria-live="polite" className="text-sm font-semibold">
                {selected.size === 0
                  ? 'No companies selected'
                  : `${selected.size.toLocaleString()} ${selected.size === 1 ? 'company' : 'companies'} selected`}
              </p>
              {selected.size > 0 && !running && (
                <Button variant="ghost" size="sm" onClick={clearSelection}>
                  Clear selection
                </Button>
              )}
            </div>

            <Pagination
              page={companies.data?.page ?? 0}
              totalPages={totalPages}
              totalElements={totalElements}
              itemLabel="companies"
              onPageChange={(p) => setFilters((f) => ({ ...f, page: p }))}
            />

            {hasRunResults && (
              <p className="text-sm text-muted-foreground" aria-live="polite">
                {running
                  ? `Running… ${runSummary.done} of ${runSummary.total} done`
                  : `Done: ${runSummary.success} succeeded, ${runSummary.partial} partial, ${runSummary.failed} failed`}
              </p>
            )}

            {canRun ? (
              <div className="flex justify-end">
                <Button
                  onClick={() => setConfirming(true)}
                  disabled={selected.size === 0 || providers.size === 0 || running}
                >
                  {running ? <Loader2 className="animate-spin" /> : <Sparkles />}
                  Run Enrichment
                </Button>
              </div>
            ) : (
              <p className="text-right text-sm text-muted-foreground">
                Your role can view enrichment results but not run enrichment.
              </p>
            )}
          </>
        )}
      </CardContent>

      <Dialog open={confirming} onOpenChange={(open) => !open && setConfirming(false)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Run enrichment?</DialogTitle>
            <DialogDescription>
              {selected.size.toLocaleString()} {selected.size === 1 ? 'company' : 'companies'} will be enriched via{' '}
              {[...providers].join(', ')}.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-3 text-sm">
            <p className="text-muted-foreground leading-relaxed">
              Each company is enriched independently — a failure on one does not stop the others. Progress and
              per-company results appear in the table once started.
            </p>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setConfirming(false)}>Cancel</Button>
            <Button onClick={runBatch}>Run Enrichment</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </Card>
  )
}

function lastEnrichedSummary(company: Company): string {
  const primaryPhone = company.phones?.find(p => p.is_primary)
  const dates = [company.google_last_enriched_at, company.website_last_enriched_at, primaryPhone?.enriched_at]
    .filter((d): d is string => !!d)
    .sort()
  const latest = dates[dates.length - 1]
  if (!latest) return 'Never'
  return new Date(latest).toLocaleDateString()
}

function summarizeRuns(states: Map<string, RunState>) {
  let success = 0
  let partial = 0
  let failed = 0
  let done = 0
  for (const state of states.values()) {
    if (state.status === 'success') { success++; done++ }
    else if (state.status === 'partial') { partial++; done++ }
    else if (state.status === 'failed') { failed++; done++ }
  }
  return { total: states.size, done, success, partial, failed }
}

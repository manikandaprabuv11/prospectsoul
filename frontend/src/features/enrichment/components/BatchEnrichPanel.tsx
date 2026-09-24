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
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Pagination } from '@/shared/components/Pagination'
import { Skeleton } from '@/components/ui/skeleton'
import { CheckCircle2, Loader2, Sparkles, XCircle } from 'lucide-react'
import { useState } from 'react'
import { useCompanies } from '@/features/company/hooks'
import type { Company } from '@/features/company/types'
import { queryClient } from '@/lib/query-client'
import { enrichmentApi } from '../api'

const PAGE_SIZE = 25

/** Same provider set the single-company Enrich button offers. */
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

const RUN_STATUS_COLOR: Record<RunStatus, string> = {
  queued: 'bg-gray-100 text-gray-800',
  running: 'bg-blue-100 text-blue-800',
  success: 'bg-green-100 text-green-800',
  partial: 'bg-yellow-100 text-yellow-800',
  failed: 'bg-red-100 text-red-800',
}

interface Props {
  /** False for read-only roles: the panel explains instead of offering to run. */
  canRun: boolean
}

/**
 * "Run Enrichment" selection panel — mirrors the Verify workspace's
 * VerifyNewCompaniesPanel pattern: filter a paginated company table,
 * multi-select with checkboxes, then run in bulk with a confirmation
 * dialog and visible per-company progress.
 *
 * <p>The backend only exposes a single-company enrich endpoint
 * (`POST /api/v1/companies/{id}/enrich`), so a "batch" here is N client-side
 * calls, one per selected company, run with limited concurrency and tracked
 * individually so a failure on one company never hides the others.
 */
export function BatchEnrichPanel({ canRun }: Props) {
  const [search, setSearch] = useState('')
  const [appliedSearch, setAppliedSearch] = useState<string | undefined>(undefined)
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [providers, setProviders] = useState<Set<string>>(new Set(PROVIDERS.map((p) => p.key)))
  const [confirming, setConfirming] = useState(false)
  const [running, setRunning] = useState(false)
  const [runStates, setRunStates] = useState<Map<string, RunState>>(new Map())

  const companies = useCompanies({
    q: appliedSearch,
    page,
    size: PAGE_SIZE,
    sort: 'createdAt',
    sort_dir: 'desc',
  })

  const rows = companies.data?.content ?? []
  const allSelected = rows.length > 0 && rows.every((row) => selected.has(row.id))
  const someSelected = rows.some((row) => selected.has(row.id))

  function applySearch() {
    setAppliedSearch(search.trim() || undefined)
    setPage(0)
  }

  function resetSearch() {
    setSearch('')
    setAppliedSearch(undefined)
    setPage(0)
  }

  function toggleRow(id: string, checked: boolean) {
    setSelected((current) => {
      const next = new Set(current)
      if (checked) next.add(id)
      else next.delete(id)
      return next
    })
  }

  function toggleCurrentPage(checked: boolean) {
    setSelected((current) => {
      const next = new Set(current)
      for (const row of rows) {
        if (checked) next.add(row.id)
        else next.delete(row.id)
      }
      return next
    })
  }

  function toggleProvider(key: string, checked: boolean) {
    setProviders((current) => {
      const next = new Set(current)
      if (checked) next.add(key)
      else next.delete(key)
      return next
    })
  }

  /** Runs enrichment for every selected company, CONCURRENCY at a time. */
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
        {/* --- filters --- */}
        <div className="flex flex-wrap items-end gap-3">
          <div className="min-w-48 flex-1 space-y-1">
            <Label htmlFor="enrich-search">Search</Label>
            <Input
              id="enrich-search"
              placeholder="Company name, city or website"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter') applySearch()
              }}
            />
          </div>
          <div className="flex gap-2">
            <Button onClick={applySearch}>Apply</Button>
            <Button variant="outline" onClick={resetSearch}>
              Reset
            </Button>
          </div>
        </div>

        {/* --- provider selection --- */}
        <div className="flex flex-wrap items-center gap-4 rounded-md border p-3">
          <span className="text-sm font-medium">Providers</span>
          {PROVIDERS.map((provider) => (
            <label key={provider.key} className="flex items-center gap-2 text-sm">
              <Checkbox
                checked={providers.has(provider.key)}
                onCheckedChange={(checked) => toggleProvider(provider.key, checked === true)}
                disabled={running}
              />
              {provider.label}
            </label>
          ))}
        </div>

        {/* --- table --- */}
        {companies.isLoading ? (
          <div className="space-y-2">
            {Array.from({ length: 5 }).map((_, index) => (
              <Skeleton key={index} className="h-11 w-full" />
            ))}
          </div>
        ) : companies.isError ? (
          <div role="alert" className="rounded-lg border border-destructive/50 p-6 text-center text-sm text-destructive">
            {companies.error instanceof Error ? companies.error.message : 'Could not load companies.'}
          </div>
        ) : rows.length === 0 ? (
          <div className="rounded-lg border p-10 text-center">
            <p className="font-medium">No companies match this search</p>
            <p className="mt-1 text-sm text-muted-foreground">Try a different name, city or website.</p>
          </div>
        ) : (
          <>
            <div className="overflow-x-auto rounded-lg border">
              <table className="w-full text-sm">
                <caption className="sr-only">Companies available for enrichment</caption>
                <thead>
                  <tr className="border-b bg-muted/50">
                    <th scope="col" className="w-10 px-3 py-2.5 text-left">
                      <Checkbox
                        checked={allSelected ? true : someSelected ? 'indeterminate' : false}
                        onCheckedChange={(checked) => toggleCurrentPage(checked === true)}
                        disabled={!canRun || running || rows.length === 0}
                        aria-label="Select all companies on this page"
                      />
                    </th>
                    <th scope="col" className="px-3 py-2.5 text-left font-medium text-muted-foreground">Company</th>
                    <th scope="col" className="px-3 py-2.5 text-left font-medium text-muted-foreground">Location</th>
                    <th scope="col" className="px-3 py-2.5 text-left font-medium text-muted-foreground">Industry</th>
                    <th scope="col" className="px-3 py-2.5 text-left font-medium text-muted-foreground">Last enriched</th>
                    {hasRunResults && (
                      <th scope="col" className="px-3 py-2.5 text-left font-medium text-muted-foreground">Result</th>
                    )}
                  </tr>
                </thead>
                <tbody>
                  {rows.map((row) => {
                    const runState = runStates.get(row.id)
                    return (
                      <tr key={row.id} className="border-b last:border-0 hover:bg-muted/30">
                        <td className="px-3 py-2.5">
                          <Checkbox
                            checked={selected.has(row.id)}
                            onCheckedChange={(checked) => toggleRow(row.id, checked === true)}
                            disabled={!canRun || running}
                            aria-label={`Select ${row.canonical_name}`}
                          />
                        </td>
                        <td className="px-3 py-2.5 font-medium">{row.canonical_name}</td>
                        <td className="px-3 py-2.5">{row.city ?? '—'}</td>
                        <td className="px-3 py-2.5">{row.industry ?? '—'}</td>
                        <td className="px-3 py-2.5 text-xs text-muted-foreground">{lastEnrichedSummary(row)}</td>
                        {hasRunResults && (
                          <td className="px-3 py-2.5">
                            {runState ? (
                              <div className="flex items-center gap-1.5">
                                {runState.status === 'running' && <Loader2 className="size-3 animate-spin" />}
                                {runState.status === 'success' && <CheckCircle2 className="size-3 text-green-600" />}
                                {runState.status === 'failed' && <XCircle className="size-3 text-red-600" />}
                                <Badge className={`text-xs ${RUN_STATUS_COLOR[runState.status]}`}>
                                  {RUN_STATUS_LABEL[runState.status]}
                                </Badge>
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
              <p aria-live="polite" className="text-sm font-medium">
                {selected.size === 0
                  ? 'No companies selected'
                  : `${selected.size} ${selected.size === 1 ? 'company' : 'companies'} selected`}
              </p>
              {selected.size > 0 && !running && (
                <Button variant="ghost" size="sm" onClick={() => setSelected(new Set())}>
                  Clear selection
                </Button>
              )}
            </div>

            <Pagination
              page={companies.data?.page ?? 0}
              totalPages={companies.data?.total_pages ?? 1}
              totalElements={companies.data?.total_elements}
              itemLabel="companies"
              onPageChange={setPage}
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
              {selected.size} {selected.size === 1 ? 'company' : 'companies'} will be enriched via{' '}
              {[...providers].join(', ')}.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-3 text-sm">
            <p className="text-muted-foreground">
              Each company is enriched independently — a failure on one does not stop the others. Progress and
              per-company results appear in the table once started.
            </p>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setConfirming(false)}>
              Cancel
            </Button>
            <Button onClick={runBatch}>Run Enrichment</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </Card>
  )
}

function lastEnrichedSummary(company: Company): string {
  const dates = [company.google_last_enriched_at, company.website_last_enriched_at, company.primary_phone_last_enriched_at]
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

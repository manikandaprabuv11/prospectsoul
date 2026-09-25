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
import { Select } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { Badge } from '@/components/ui/badge'
import { Pagination } from '@/shared/components/Pagination'
import { Search } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useAddedByOptions, useEligibleCompanies, useStartVerification } from '../hooks'
import type { EligibleCompany } from '../types'
import { defaultDateRange, formatDate, formatPhone, problemDetail } from './format'

const PAGE_SIZE = 25

interface Props {
  canStart: boolean
  batchRunning: boolean
}

export function VerifyNewCompaniesPanel({ canStart, batchRunning }: Props) {
  const initialRange = useMemo(() => defaultDateRange(), [])

  const [addedBy, setAddedBy] = useState('')
  const [dateFrom, setDateFrom] = useState(initialRange.from)
  const [dateTo, setDateTo] = useState(initialRange.to)
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)

  const [applied, setApplied] = useState({
    added_by: undefined as string | undefined,
    date_from: initialRange.from as string | undefined,
    date_to: initialRange.to as string | undefined,
    q: undefined as string | undefined,
  })

  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [confirming, setConfirming] = useState(false)

  const addedByOptions = useAddedByOptions()
  const eligible = useEligibleCompanies({
    ...applied,
    page,
    size: PAGE_SIZE,
    sort: 'createdAt',
    sort_dir: 'desc',
  })
  const startVerification = useStartVerification()

  const rows = eligible.data?.content ?? []
  const selectableRows = rows.filter(isSelectable)
  const allSelectableSelected =
    selectableRows.length > 0 && selectableRows.every((row) => selected.has(row.id))
  const someSelectableSelected = selectableRows.some((row) => selected.has(row.id))

  const dateRangeInvalid = dateFrom !== '' && dateTo !== '' && dateFrom > dateTo

  function apply() {
    if (dateRangeInvalid) return
    setApplied({
      added_by: addedBy || undefined,
      date_from: dateFrom || undefined,
      date_to: dateTo || undefined,
      q: search.trim() || undefined,
    })
    setPage(0)
    setSelected(new Set())
  }

  function reset() {
    setAddedBy('')
    setDateFrom(initialRange.from)
    setDateTo(initialRange.to)
    setSearch('')
    setApplied({
      added_by: undefined,
      date_from: initialRange.from,
      date_to: initialRange.to,
      q: undefined,
    })
    setPage(0)
    setSelected(new Set())
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
      for (const row of selectableRows) {
        if (checked) next.add(row.id)
        else next.delete(row.id)
      }
      return next
    })
  }

  function start() {
    startVerification.mutate(
      {
        company_ids: [...selected],
        added_by: applied.added_by,
        date_from: applied.date_from,
        date_to: applied.date_to,
      },
      {
        onSuccess: () => {
          setConfirming(false)
          setSelected(new Set())
        },
      },
    )
  }

  const startErrorDetail = startVerification.error
    ? problemDetail(startVerification.error, 'Could not start verification.')
    : null

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between gap-3 pb-3">
        <CardTitle>Verify New Companies</CardTitle>
        <Badge variant="secondary">Unverified only</Badge>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="rounded-xl border border-border bg-card p-4 shadow-card">
          <div className="flex flex-wrap items-end gap-3">
            <div className="space-y-1">
              <Label htmlFor="verify-added-by" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Added by</Label>
              <Select
                id="verify-added-by"
                className="w-48"
                value={addedBy}
                onChange={(event) => setAddedBy(event.target.value)}
              >
                <option value="">All</option>
                {(addedByOptions.data ?? []).map((option) => (
                  <option key={option.id} value={option.id}>
                    {option.name} ({option.company_count})
                  </option>
                ))}
              </Select>
            </div>
            <div className="space-y-1">
              <Label htmlFor="verify-date-from" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">From</Label>
              <Input
                id="verify-date-from"
                type="date"
                className="w-40"
                value={dateFrom}
                onChange={(event) => setDateFrom(event.target.value)}
              />
            </div>
            <div className="space-y-1">
              <Label htmlFor="verify-date-to" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">To</Label>
              <Input
                id="verify-date-to"
                type="date"
                className="w-40"
                value={dateTo}
                onChange={(event) => setDateTo(event.target.value)}
              />
            </div>
            <div className="min-w-48 flex-1 space-y-1">
              <Label htmlFor="verify-search" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Search</Label>
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" />
                <Input
                  id="verify-search"
                  className="pl-9"
                  placeholder="Company name or phone"
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter') apply()
                  }}
                />
              </div>
            </div>
            <div className="flex gap-2">
              <Button onClick={apply} disabled={dateRangeInvalid}>
                Apply
              </Button>
              <Button variant="outline" onClick={reset}>
                Reset
              </Button>
            </div>
          </div>
        </div>

        {dateRangeInvalid && (
          <p role="alert" className="text-sm font-medium text-destructive">
            Date from must not be after date to.
          </p>
        )}

        {eligible.isLoading ? (
          <div className="space-y-2">
            {Array.from({ length: 5 }).map((_, index) => (
              <Skeleton key={index} className="h-11 w-full" />
            ))}
          </div>
        ) : eligible.isError ? (
          <div role="alert" className="rounded-xl border border-destructive/30 bg-destructive/5 p-6 text-center text-sm font-medium text-destructive">
            {problemDetail(eligible.error, 'Could not load companies.')}
          </div>
        ) : rows.length === 0 ? (
          <div className="rounded-xl border border-dashed border-border bg-surface-1/50 py-12 text-center">
            <p className="font-semibold">No unverified companies match these filters</p>
            <p className="mt-1 text-sm text-muted-foreground">
              Every company added by this user in this date range is already verified, or nothing
              was added in the range. Widen the dates or choose <strong>All</strong> under Added by.
            </p>
            <Button variant="outline" className="mt-4" onClick={reset}>
              Reset filters
            </Button>
          </div>
        ) : (
          <>
            <div className="overflow-x-auto rounded-xl border border-border shadow-card">
              <table className="w-full text-sm">
                <caption className="sr-only">
                  Unverified companies matching the selected filters
                </caption>
                <thead>
                  <tr className="border-b border-border bg-surface-1">
                    <th scope="col" className="w-10 px-3 py-2.5 text-left">
                      <Checkbox
                        checked={
                          allSelectableSelected
                            ? true
                            : someSelectableSelected
                              ? 'indeterminate'
                              : false
                        }
                        onCheckedChange={(checked) => toggleCurrentPage(checked === true)}
                        disabled={!canStart || selectableRows.length === 0}
                        aria-label="Select all companies on this page"
                      />
                    </th>
                    <th scope="col" className="px-3 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Company</th>
                    <th scope="col" className="px-3 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Added by</th>
                    <th scope="col" className="px-3 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Added date</th>
                    <th scope="col" className="px-3 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Phone</th>
                    <th scope="col" className="px-3 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {rows.map((row) => {
                    const selectable = isSelectable(row)
                    return (
                      <tr key={row.id} className="group/row hover:bg-surface-1/50 transition-colors">
                        <td className="px-3 py-2.5">
                          <Checkbox
                            checked={selected.has(row.id)}
                            onCheckedChange={(checked) => toggleRow(row.id, checked === true)}
                            disabled={!canStart || !selectable}
                            aria-label={`Select ${row.canonical_name}`}
                          />
                        </td>
                        <td className="px-3 py-2.5 font-semibold">{row.canonical_name}</td>
                        <td className="px-3 py-2.5">{row.added_by_name ?? '—'}</td>
                        <td className="px-3 py-2.5 text-muted-foreground tabular-nums">{formatDate(row.added_at)}</td>
                        <td className="px-3 py-2.5 tabular-nums">{formatPhone(row.primary_phone_normalized)}</td>
                        <td className="px-3 py-2.5">
                          {row.in_flight ? (
                            <Badge variant="warning">Being verified</Badge>
                          ) : !row.phone_usable ? (
                            <Badge variant="outline">No usable phone</Badge>
                          ) : (
                            <Badge variant="secondary">
                              {row.verification_status === 'INVALIDATED' ? 'Needs re-verify' : 'Unverified'}
                            </Badge>
                          )}
                        </td>
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
                  : `${selected.size} ${selected.size === 1 ? 'company' : 'companies'} selected`}
              </p>
              {selected.size > 0 && (
                <Button variant="ghost" size="sm" onClick={() => setSelected(new Set())}>
                  Clear selection
                </Button>
              )}
            </div>

            <Pagination
              page={eligible.data?.page ?? 0}
              totalPages={eligible.data?.total_pages ?? 1}
              totalElements={eligible.data?.total_elements}
              itemLabel="eligible"
              onPageChange={setPage}
            />

            {canStart ? (
              <div className="flex flex-wrap items-center justify-end gap-3">
                {batchRunning && (
                  <p className="text-sm text-muted-foreground">
                    A verification is already running. Wait for it to finish before starting another.
                  </p>
                )}
                <Button
                  onClick={() => setConfirming(true)}
                  disabled={selected.size === 0 || batchRunning || startVerification.isPending}
                >
                  Start Verification
                </Button>
              </div>
            ) : (
              <p className="text-right text-sm text-muted-foreground">
                Your role can view verification results but not start a verification.
              </p>
            )}
          </>
        )}

        {startErrorDetail && (
          <div role="alert" className="rounded-xl border border-destructive/30 bg-destructive/5 p-3 text-sm font-medium text-destructive">
            {startErrorDetail}
          </div>
        )}
      </CardContent>

      <Dialog open={confirming} onOpenChange={(open) => !open && setConfirming(false)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Start verification?</DialogTitle>
            <DialogDescription>
              {selected.size} {selected.size === 1 ? 'company' : 'companies'} will be checked
              through the phone-verification provider.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-3 text-sm">
            <p className="leading-relaxed">
              Verification runs in the background on the server. You can leave this page, navigate
              elsewhere or close the browser — progress keeps going and is restored when you come
              back.
            </p>
            <p className="text-muted-foreground leading-relaxed">
              A company is marked <strong>Verified</strong> only when the provider reports a valid
              number on a mobile line. Everything else is recorded as a failure with its reason, and
              the company stays unverified. This does not confirm ownership of the number.
            </p>
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setConfirming(false)}
              disabled={startVerification.isPending}
            >
              Cancel
            </Button>
            <Button onClick={start} disabled={startVerification.isPending}>
              {startVerification.isPending ? 'Starting…' : 'Start Verification'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </Card>
  )
}

function isSelectable(row: EligibleCompany): boolean {
  return row.phone_usable && !row.in_flight
}

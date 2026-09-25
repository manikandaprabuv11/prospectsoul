import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { Pagination } from '@/shared/components/Pagination'
import { ArrowDown, ArrowUp, Search } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'
import { useAddedByOptions, useVerifiedCompanies } from '../hooks'
import { formatDate, formatDateTime, formatLineType, formatPhone, problemDetail } from './format'

const PAGE_SIZE = 25

const SORTABLE = [
  { field: 'verifiedAt', label: 'Verified at' },
  { field: 'canonicalName', label: 'Company' },
  { field: 'city', label: 'City' },
  { field: 'createdAt', label: 'Added at' },
] as const

type SortField = (typeof SORTABLE)[number]['field']

export function VerifiedCompaniesTable() {
  const [search, setSearch] = useState('')
  const [appliedSearch, setAppliedSearch] = useState<string | undefined>(undefined)
  const [verifiedBy, setVerifiedBy] = useState('')
  const [addedBy, setAddedBy] = useState('')
  const [verifiedFrom, setVerifiedFrom] = useState('')
  const [verifiedTo, setVerifiedTo] = useState('')
  const [sort, setSort] = useState<SortField>('verifiedAt')
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('desc')
  const [page, setPage] = useState(0)

  const people = useAddedByOptions()
  const dateRangeInvalid = verifiedFrom !== '' && verifiedTo !== '' && verifiedFrom > verifiedTo

  const companies = useVerifiedCompanies({
    q: appliedSearch,
    verified_by: verifiedBy || undefined,
    added_by: addedBy || undefined,
    verified_from: dateRangeInvalid ? undefined : verifiedFrom || undefined,
    verified_to: dateRangeInvalid ? undefined : verifiedTo || undefined,
    page,
    size: PAGE_SIZE,
    sort,
    sort_dir: sortDir,
  })

  const rows = companies.data?.content ?? []

  function toggleSort(field: SortField) {
    if (sort === field) {
      setSortDir((current) => (current === 'asc' ? 'desc' : 'asc'))
    } else {
      setSort(field)
      setSortDir('desc')
    }
    setPage(0)
  }

  function resetFilters() {
    setSearch('')
    setAppliedSearch(undefined)
    setVerifiedBy('')
    setAddedBy('')
    setVerifiedFrom('')
    setVerifiedTo('')
    setPage(0)
  }

  return (
    <Card>
      <CardHeader className="pb-3">
        <CardTitle>Verified Companies</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="rounded-xl border border-border bg-card p-4 shadow-card">
          <div className="flex flex-wrap items-end gap-3">
            <div className="min-w-48 flex-1 space-y-1">
              <Label htmlFor="verified-search" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Search</Label>
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground" />
                <Input
                  id="verified-search"
                  className="pl-9"
                  placeholder="Company, phone, city or email"
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter') {
                      setAppliedSearch(search.trim() || undefined)
                      setPage(0)
                    }
                  }}
                />
              </div>
            </div>
            <div className="space-y-1">
              <Label htmlFor="verified-by" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Verified by</Label>
              <Select
                id="verified-by"
                className="w-44"
                value={verifiedBy}
                onChange={(event) => {
                  setVerifiedBy(event.target.value)
                  setPage(0)
                }}
              >
                <option value="">Anyone</option>
                {(people.data ?? []).map((option) => (
                  <option key={option.id} value={option.id}>
                    {option.name}
                  </option>
                ))}
              </Select>
            </div>
            <div className="space-y-1">
              <Label htmlFor="verified-added-by" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Added by</Label>
              <Select
                id="verified-added-by"
                className="w-44"
                value={addedBy}
                onChange={(event) => {
                  setAddedBy(event.target.value)
                  setPage(0)
                }}
              >
                <option value="">Anyone</option>
                {(people.data ?? []).map((option) => (
                  <option key={option.id} value={option.id}>
                    {option.name}
                  </option>
                ))}
              </Select>
            </div>
            <div className="space-y-1">
              <Label htmlFor="verified-from" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">From</Label>
              <Input
                id="verified-from"
                type="date"
                className="w-40"
                value={verifiedFrom}
                onChange={(event) => {
                  setVerifiedFrom(event.target.value)
                  setPage(0)
                }}
              />
            </div>
            <div className="space-y-1">
              <Label htmlFor="verified-to" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">To</Label>
              <Input
                id="verified-to"
                type="date"
                className="w-40"
                value={verifiedTo}
                onChange={(event) => {
                  setVerifiedTo(event.target.value)
                  setPage(0)
                }}
              />
            </div>
            <div className="flex gap-2">
              <Button
                onClick={() => {
                  setAppliedSearch(search.trim() || undefined)
                  setPage(0)
                }}
              >
                Search
              </Button>
              <Button variant="outline" onClick={resetFilters}>
                Reset
              </Button>
            </div>
          </div>
        </div>

        {dateRangeInvalid && (
          <p role="alert" className="text-sm font-medium text-destructive">
            Verified from must not be after verified to.
          </p>
        )}

        {companies.isLoading ? (
          <div className="space-y-2">
            {Array.from({ length: 5 }).map((_, index) => (
              <Skeleton key={index} className="h-11 w-full" />
            ))}
          </div>
        ) : companies.isError ? (
          <div role="alert" className="rounded-xl border border-destructive/30 bg-destructive/5 p-6 text-center text-sm font-medium text-destructive">
            {problemDetail(companies.error, 'Could not load verified companies.')}
          </div>
        ) : rows.length === 0 ? (
          <div className="rounded-xl border border-dashed border-border bg-surface-1/50 py-12 text-center">
            <p className="font-semibold">No verified companies yet</p>
            <p className="mt-1 text-sm text-muted-foreground">
              Companies appear here once a verification confirms their phone is a valid mobile
              number. Start with <strong>Verify New Companies</strong> above.
            </p>
          </div>
        ) : (
          <>
            <div className="overflow-x-auto rounded-xl border border-border shadow-card">
              <table className="w-full text-sm">
                <caption className="sr-only">Companies with a verified phone number</caption>
                <thead>
                  <tr className="border-b border-border bg-surface-1">
                    {SORTABLE.map((column) => (
                      <th key={column.field} scope="col" className="px-3 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
                        <button
                          type="button"
                          className="inline-flex items-center gap-1 transition-colors hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                          onClick={() => toggleSort(column.field)}
                          aria-label={`Sort by ${column.label}`}
                        >
                          {column.label}
                          {sort === column.field &&
                            (sortDir === 'asc' ? (
                              <ArrowUp className="size-3" />
                            ) : (
                              <ArrowDown className="size-3" />
                            ))}
                        </button>
                      </th>
                    ))}
                    <th scope="col" className="px-3 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Verified by</th>
                    <th scope="col" className="px-3 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Added by</th>
                    <th scope="col" className="px-3 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Phone</th>
                    <th scope="col" className="px-3 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Line type</th>
                    <th scope="col" className="px-3 py-2.5 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Carrier</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {rows.map((company) => (
                    <tr key={company.id} className="group/row hover:bg-surface-1/50 transition-colors">
                      <td className="px-3 py-2.5 text-muted-foreground tabular-nums">
                        {formatDateTime(company.verified_at)}
                      </td>
                      <td className="px-3 py-2.5 font-semibold">
                        <Link to={`/companies/${company.id}`} className="text-primary hover:underline transition-colors">
                          {company.canonical_name}
                        </Link>
                      </td>
                      <td className="px-3 py-2.5">{company.city ?? '—'}</td>
                      <td className="px-3 py-2.5 text-muted-foreground tabular-nums">{formatDate(company.added_at)}</td>
                      <td className="px-3 py-2.5">{company.verified_by_name ?? '—'}</td>
                      <td className="px-3 py-2.5">{company.added_by_name ?? '—'}</td>
                      <td className="px-3 py-2.5 tabular-nums">{formatPhone(company.verified_phone)}</td>
                      <td className="px-3 py-2.5">{formatLineType(company.line_type)}</td>
                      <td className="px-3 py-2.5">{company.carrier_name ?? '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <Pagination
              page={companies.data?.page ?? 0}
              totalPages={companies.data?.total_pages ?? 1}
              totalElements={companies.data?.total_elements}
              itemLabel="verified"
              onPageChange={setPage}
            />
          </>
        )}
      </CardContent>
    </Card>
  )
}

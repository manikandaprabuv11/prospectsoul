import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { Pagination } from '@/shared/components/Pagination'
import { ArrowDown, ArrowUp } from 'lucide-react'
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

/**
 * The Verify page's bottom table.
 *
 * <p>Reads `GET /api/v1/verifications/companies`, which returns companies whose
 * canonical status is VERIFIED and nothing else — the invariant is enforced in
 * SQL, so this table cannot show an unverified company.
 */
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
        <div className="flex flex-wrap items-end gap-3">
          <div className="min-w-48 flex-1 space-y-1">
            <Label htmlFor="verified-search">Search</Label>
            <Input
              id="verified-search"
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
          <div className="space-y-1">
            <Label htmlFor="verified-by">Verified by</Label>
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
            <Label htmlFor="verified-added-by">Added by</Label>
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
            <Label htmlFor="verified-from">Verified from</Label>
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
            <Label htmlFor="verified-to">Verified to</Label>
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

        {dateRangeInvalid && (
          <p role="alert" className="text-sm text-destructive">
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
          <div
            role="alert"
            className="rounded-lg border border-destructive/50 p-6 text-center text-sm text-destructive"
          >
            {problemDetail(companies.error, 'Could not load verified companies.')}
          </div>
        ) : rows.length === 0 ? (
          <div className="rounded-lg border p-10 text-center">
            <p className="font-medium">No verified companies yet</p>
            <p className="mt-1 text-sm text-muted-foreground">
              Companies appear here once a verification confirms their phone is a valid mobile
              number. Start with <strong>Verify New Companies</strong> above.
            </p>
          </div>
        ) : (
          <>
            <div className="overflow-x-auto rounded-lg border">
              <table className="w-full text-sm">
                <caption className="sr-only">Companies with a verified phone number</caption>
                <thead>
                  <tr className="border-b bg-muted/50">
                    {SORTABLE.map((column) => (
                      <th key={column.field} scope="col" className="px-3 py-2.5 text-left font-medium text-muted-foreground">
                        <button
                          type="button"
                          className="inline-flex items-center gap-1 hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
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
                    <th scope="col" className="px-3 py-2.5 text-left font-medium text-muted-foreground">Verified by</th>
                    <th scope="col" className="px-3 py-2.5 text-left font-medium text-muted-foreground">Added by</th>
                    <th scope="col" className="px-3 py-2.5 text-left font-medium text-muted-foreground">Phone</th>
                    <th scope="col" className="px-3 py-2.5 text-left font-medium text-muted-foreground">Line type</th>
                    <th scope="col" className="px-3 py-2.5 text-left font-medium text-muted-foreground">Carrier</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((company) => (
                    <tr key={company.id} className="border-b last:border-0 hover:bg-muted/30">
                      <td className="px-3 py-2.5 text-muted-foreground">
                        {formatDateTime(company.verified_at)}
                      </td>
                      <td className="px-3 py-2.5 font-medium">
                        <Link to={`/companies/${company.id}`} className="text-primary hover:underline">
                          {company.canonical_name}
                        </Link>
                      </td>
                      <td className="px-3 py-2.5">{company.city ?? '—'}</td>
                      <td className="px-3 py-2.5 text-muted-foreground">{formatDate(company.added_at)}</td>
                      <td className="px-3 py-2.5">{company.verified_by_name ?? '—'}</td>
                      <td className="px-3 py-2.5">{company.added_by_name ?? '—'}</td>
                      <td className="px-3 py-2.5">{formatPhone(company.verified_phone)}</td>
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

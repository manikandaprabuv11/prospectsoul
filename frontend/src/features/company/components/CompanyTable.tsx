import { Button } from '@/components/ui/button'
import { StatusChip } from '@/components/feedback/StatusChip'
import { EmptyState } from '@/components/feedback/EmptyState'
import { ArrowDown, ArrowUp, ArrowUpDown, Building2, Eye, Pencil } from 'lucide-react'
import { Link } from 'react-router'
import type { Company } from '../types'

interface Props {
  companies: Company[]
  onSort: (field: string) => void
  sortField: string
  sortDir: string
}

const columns = [
  { key: 'canonical_name', apiKey: 'canonicalName', label: 'Company', sortable: true },
  { key: 'primary_phone_normalized', label: 'Phone', sortable: false },
  { key: 'email', label: 'Email', sortable: false },
  { key: 'city', label: 'City', sortable: true },
  { key: 'state', label: 'State', sortable: true },
  { key: 'industry', label: 'Industry', sortable: true },
  { key: 'source', label: 'Source', sortable: true },
  { key: 'pipeline_state', label: 'Pipeline', sortable: true },
  { key: 'verification_status', label: 'Verification', sortable: true },
  { key: 'created_at', apiKey: 'createdAt', label: 'Created', sortable: true },
] as const

export function CompanyTable({ companies, onSort, sortField, sortDir }: Props) {
  if (companies.length === 0) {
    return (
      <EmptyState
        icon={<Building2 className="size-6" />}
        title="No companies match this filter"
        description="Try clearing filters, importing a new file, or creating a company manually."
        action={
          <div className="flex gap-2">
            <Button variant="outline" size="sm" asChild>
              <Link to="/imports/new">Import a file</Link>
            </Button>
            <Button size="sm" asChild>
              <Link to="/companies/new">New company</Link>
            </Button>
          </div>
        }
      />
    )
  }

  return (
    <div className="overflow-hidden rounded-lg border border-border/70 bg-card shadow-xs">
      <div className="overflow-x-auto">
        <table className="w-full text-sm" data-tabular="true">
          <thead className="bg-muted/40">
            <tr className="border-b border-border/70">
              {columns.map((col) => {
                const sortKey = 'apiKey' in col ? col.apiKey : col.key
                const active = col.sortable && sortField === sortKey
                return (
                  <th
                    key={col.key}
                    scope="col"
                    className="px-3 py-2.5 text-left text-xs font-semibold uppercase tracking-wide text-muted-foreground"
                  >
                    {col.sortable ? (
                      <button
                        type="button"
                        onClick={() => onSort(sortKey)}
                        className="inline-flex items-center gap-1 hover:text-foreground transition-colors"
                      >
                        <span>{col.label}</span>
                        {active ? (
                          sortDir === 'asc'
                            ? <ArrowUp className="size-3" aria-hidden="true" />
                            : <ArrowDown className="size-3" aria-hidden="true" />
                        ) : (
                          <ArrowUpDown className="size-3 opacity-40" aria-hidden="true" />
                        )}
                      </button>
                    ) : (
                      <span>{col.label}</span>
                    )}
                  </th>
                )
              })}
              <th scope="col" className="px-3 py-2.5 text-right text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border/70">
            {companies.map((company) => (
              <tr key={company.id} className="hover:bg-muted/30 transition-colors">
                <td className="px-3 py-2.5 max-w-[260px]">
                  <Link
                    to={`/companies/${company.id}`}
                    className="font-medium text-foreground hover:text-primary transition-colors block truncate"
                  >
                    {company.canonical_name}
                  </Link>
                  <p className="text-xs text-muted-foreground truncate">
                    {company.website_domain || company.normalized_name}
                  </p>
                </td>
                <td className="px-3 py-2.5 text-muted-foreground">{company.primary_phone_normalized ?? '—'}</td>
                <td className="px-3 py-2.5 text-muted-foreground truncate max-w-[220px]">{company.email ?? '—'}</td>
                <td className="px-3 py-2.5">{company.city ?? '—'}</td>
                <td className="px-3 py-2.5">{company.state ?? '—'}</td>
                <td className="px-3 py-2.5">{company.industry ?? '—'}</td>
                <td className="px-3 py-2.5">
                  {company.source ? (
                    <span className="inline-flex items-center rounded-md bg-muted px-1.5 py-0.5 text-xs font-medium text-foreground">
                      {company.source}
                    </span>
                  ) : '—'}
                </td>
                <td className="px-3 py-2.5"><StatusChip kind="pipeline" value={company.pipeline_state} /></td>
                <td className="px-3 py-2.5"><StatusChip kind="verification" value={company.verification_status} /></td>
                <td className="px-3 py-2.5 text-muted-foreground whitespace-nowrap">
                  {new Date(company.created_at).toLocaleDateString(undefined, {
                    year: 'numeric', month: 'short', day: 'numeric',
                  })}
                </td>
                <td className="px-3 py-2.5">
                  <div className="flex items-center justify-end gap-0.5">
                    <Button variant="ghost" size="icon-sm" asChild>
                      <Link to={`/companies/${company.id}`} aria-label={`View ${company.canonical_name}`}>
                        <Eye className="size-4" />
                      </Link>
                    </Button>
                    <Button variant="ghost" size="icon-sm" asChild>
                      <Link to={`/companies/${company.id}/edit`} aria-label={`Edit ${company.canonical_name}`}>
                        <Pencil className="size-4" />
                      </Link>
                    </Button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

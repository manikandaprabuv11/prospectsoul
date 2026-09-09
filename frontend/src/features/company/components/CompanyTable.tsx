import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Eye, Pencil } from 'lucide-react'
import { Link } from 'react-router'
import type { Company } from '../types'

interface Props {
  companies: Company[]
  onSort: (field: string) => void
  sortField: string
  sortDir: string
}

const columns = [
  { key: 'canonical_name', label: 'Company Name', sortable: true },
  { key: 'primary_phone_normalized', label: 'Phone', sortable: false },
  { key: 'email', label: 'Email', sortable: false },
  { key: 'city', label: 'City', sortable: true },
  { key: 'state', label: 'State', sortable: true },
  { key: 'industry', label: 'Industry', sortable: true },
  { key: 'source', label: 'Source', sortable: true },
  { key: 'pipeline_state', label: 'Pipeline', sortable: true },
  { key: 'verification_status', label: 'Verification', sortable: true },
  { key: 'created_at', label: 'Created', sortable: true },
]

function verificationBadge(status: string) {
  switch (status) {
    case 'VERIFIED': return <Badge variant="success">Verified</Badge>
    case 'INVALIDATED': return <Badge variant="warning">Invalidated</Badge>
    default: return <Badge variant="secondary">Unverified</Badge>
  }
}

function pipelineBadge(state: string) {
  switch (state) {
    case 'IMPORTED': return <Badge variant="outline">Imported</Badge>
    case 'READY': return <Badge variant="success">Ready</Badge>
    case 'EXPORTED': return <Badge variant="default">Exported</Badge>
    case 'DISQUALIFIED': return <Badge variant="destructive">Disqualified</Badge>
    default: return <Badge variant="secondary">{state}</Badge>
  }
}

export function CompanyTable({ companies, onSort, sortField, sortDir }: Props) {
  const handleSort = (key: string) => {
    onSort(key)
  }

  return (
    <div className="overflow-x-auto rounded-lg border">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b bg-muted/50">
            {columns.map((col) => (
              <th
                key={col.key}
                className="px-3 py-2.5 text-left font-medium text-muted-foreground"
              >
                {col.sortable ? (
                  <button
                    type="button"
                    className="flex items-center gap-1 hover:text-foreground"
                    onClick={() => handleSort(col.key === 'canonical_name' ? 'canonicalName' : col.key === 'created_at' ? 'createdAt' : col.key)}
                  >
                    {col.label}
                    {sortField === col.key && (sortDir === 'asc' ? ' ↑' : ' ↓')}
                  </button>
                ) : (
                  col.label
                )}
              </th>
            ))}
            <th className="px-3 py-2.5 text-left font-medium text-muted-foreground">Actions</th>
          </tr>
        </thead>
        <tbody>
          {companies.length === 0 ? (
            <tr>
              <td colSpan={columns.length + 1} className="px-3 py-8 text-center text-muted-foreground">
                No companies found. Create one or import from a file.
              </td>
            </tr>
          ) : (
            companies.map((company) => (
              <tr key={company.id} className="border-b last:border-0 hover:bg-muted/30">
                <td className="px-3 py-2.5 font-medium">
                  <Link to={`/companies/${company.id}`} className="text-primary hover:underline">
                    {company.canonical_name}
                  </Link>
                </td>
                <td className="px-3 py-2.5">{company.primary_phone_normalized ?? '—'}</td>
                <td className="px-3 py-2.5">{company.email ?? '—'}</td>
                <td className="px-3 py-2.5">{company.city ?? '—'}</td>
                <td className="px-3 py-2.5">{company.state ?? '—'}</td>
                <td className="px-3 py-2.5">{company.industry ?? '—'}</td>
                <td className="px-3 py-2.5">{company.source ?? '—'}</td>
                <td className="px-3 py-2.5">{pipelineBadge(company.pipeline_state)}</td>
                <td className="px-3 py-2.5">{verificationBadge(company.verification_status)}</td>
                <td className="px-3 py-2.5 text-muted-foreground">
                  {new Date(company.created_at).toLocaleDateString()}
                </td>
                <td className="px-3 py-2.5">
                  <div className="flex items-center gap-1">
                    <Button variant="ghost" size="icon-xs" asChild>
                      <Link to={`/companies/${company.id}`}><Eye className="size-3.5" /></Link>
                    </Button>
                    <Button variant="ghost" size="icon-xs" asChild>
                      <Link to={`/companies/${company.id}/edit`}><Pencil className="size-3.5" /></Link>
                    </Button>
                  </div>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  )
}

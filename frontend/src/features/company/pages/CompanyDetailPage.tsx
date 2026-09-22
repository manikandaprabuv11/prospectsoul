import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { ArrowLeft, CheckCircle, Pencil } from 'lucide-react'
import { Link, useParams } from 'react-router'
import { ContactsPanel } from '@/features/contact/components/ContactsPanel'
import { CompanyNicSection } from '@/features/company/components/CompanyNicSection'
import { useCompany, useVerifyCompany } from '../hooks'

export function CompanyDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { data: company, isLoading, isError, error } = useCompany(id)
  const verifyMutation = useVerifyCompany(id ?? '')

  if (isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-48 w-full" />
      </div>
    )
  }

  if (isError) {
    return (
      <div className="rounded-lg border border-destructive/50 p-6 text-center text-sm text-destructive">
        {error instanceof Error ? error.message : 'Failed to load company'}
      </div>
    )
  }

  if (!company) return null

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="icon" asChild>
            <Link to="/companies"><ArrowLeft className="size-4" /></Link>
          </Button>
          <div>
            <h1 className="text-2xl font-semibold tracking-tight">{company.canonical_name}</h1>
            <p className="text-sm text-muted-foreground">
              {company.pipeline_state} · {company.verification_status}
            </p>
          </div>
        </div>
        <div className="flex gap-2">
          {company.verification_status !== 'VERIFIED' && (
            <Button
              variant="outline"
              onClick={() => verifyMutation.mutate()}
              disabled={verifyMutation.isPending}
            >
              <CheckCircle className="size-4" /> Verify
            </Button>
          )}
          <Button asChild>
            <Link to={`/companies/${id}/edit`}><Pencil className="size-4" /> Edit</Link>
          </Button>
        </div>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <Card>
          <CardHeader><CardTitle>Company Information</CardTitle></CardHeader>
          <CardContent>
            <dl className="grid grid-cols-[140px_1fr] gap-x-4 gap-y-2 text-sm">
              <dt className="text-muted-foreground">Name</dt>
              <dd>{company.canonical_name}</dd>
              <dt className="text-muted-foreground">Normalized</dt>
              <dd className="font-mono text-xs">{company.normalized_name}</dd>
              <dt className="text-muted-foreground">Phone</dt>
              <dd>{company.primary_phone_normalized ?? '—'}</dd>
              <dt className="text-muted-foreground">Email</dt>
              <dd>{company.email ?? '—'}</dd>
              <dt className="text-muted-foreground">Website</dt>
              <dd>{company.website_domain ?? '—'}</dd>
              <dt className="text-muted-foreground">Industry</dt>
              <dd>{company.industry ?? '—'}</dd>
              <dt className="text-muted-foreground">Size Band</dt>
              <dd>{company.size_band ?? '—'}</dd>
            </dl>
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle>Location & Status</CardTitle></CardHeader>
          <CardContent>
            <dl className="grid grid-cols-[140px_1fr] gap-x-4 gap-y-2 text-sm">
              <dt className="text-muted-foreground">City</dt>
              <dd>{company.city ?? '—'}</dd>
              <dt className="text-muted-foreground">State</dt>
              <dd>{company.state ?? '—'}</dd>
              <dt className="text-muted-foreground">Cluster</dt>
              <dd>{company.cluster ?? '—'}</dd>
              <dt className="text-muted-foreground">Source</dt>
              <dd>{company.source ?? '—'}</dd>
              <dt className="text-muted-foreground">Pipeline</dt>
              <dd><Badge variant="outline">{company.pipeline_state}</Badge></dd>
              <dt className="text-muted-foreground">Verification</dt>
              <dd>
                <Badge variant={company.verification_status === 'VERIFIED' ? 'success' : 'secondary'}>
                  {company.verification_status}
                </Badge>
              </dd>
              <dt className="text-muted-foreground">Completeness</dt>
              <dd>{company.completeness_score}%</dd>
              <dt className="text-muted-foreground">Created</dt>
              <dd>{new Date(company.created_at).toLocaleString()}</dd>
              {company.verified_by && (
                <>
                  <dt className="text-muted-foreground">Verified By</dt>
                  <dd>{company.verified_by}</dd>
                </>
              )}
            </dl>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader><CardTitle>Sales Intelligence</CardTitle></CardHeader>
        <CardContent>
          <dl className="grid grid-cols-[160px_1fr] gap-x-4 gap-y-2 text-sm md:grid-cols-[160px_1fr_160px_1fr]">
            <dt className="text-muted-foreground">Pincode</dt><dd>{company.pincode ?? '—'}</dd>
            <dt className="text-muted-foreground">District</dt><dd>{company.district ?? '—'}</dd>
            <dt className="text-muted-foreground">Region</dt><dd>{company.region ?? '—'}</dd>
            <dt className="text-muted-foreground">Turnover</dt><dd>{company.turnover ?? '—'}</dd>
            <dt className="text-muted-foreground">Employees</dt><dd>{company.employee_count ?? '—'}</dd>
            <dt className="text-muted-foreground">GST</dt><dd>{company.gst_number ?? '—'}</dd>
            <dt className="text-muted-foreground">Registration</dt><dd>{company.registration_date ?? '—'}</dd>
            <dt className="text-muted-foreground">Source ref</dt><dd className="font-mono text-xs">{company.source_reference ?? '—'}</dd>
            <dt className="text-muted-foreground">Address</dt><dd className="col-span-3">{company.address_line ?? '—'}</dd>
            <dt className="text-muted-foreground">Products</dt><dd className="col-span-3">{company.products ?? '—'}</dd>
          </dl>
        </CardContent>
      </Card>

      <CompanyNicSection companyId={id!} />

      <Card>
        <CardHeader><CardTitle>Contacts</CardTitle></CardHeader>
        <CardContent>
          <ContactsPanel companyId={id!} />
        </CardContent>
      </Card>

      {company.tags && company.tags.length > 0 && (
        <Card>
          <CardHeader><CardTitle>Tags</CardTitle></CardHeader>
          <CardContent>
            <div className="flex flex-wrap gap-2">
              {company.tags.map((tag) => (
                <Badge key={tag} variant="secondary">{tag}</Badge>
              ))}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}

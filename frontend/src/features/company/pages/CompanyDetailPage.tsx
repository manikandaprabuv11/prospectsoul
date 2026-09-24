import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { PageHeader } from '@/components/layout/PageHeader'
import { LoadingRows } from '@/components/feedback/LoadingRows'
import { ErrorState } from '@/components/feedback/ErrorState'
import { StatusChip } from '@/components/feedback/StatusChip'
import { ArrowLeft, Building2, CheckCircle2, ExternalLink, Pencil } from 'lucide-react'
import { Link, useParams } from 'react-router'
import { ContactsPanel } from '@/features/contact/components/ContactsPanel'
import { CompanyNicSection } from '@/features/company/components/CompanyNicSection'
import { EnrichButton } from '@/features/enrichment/components/EnrichButton'
import { CandidatesPanel } from '@/features/enrichment/components/CandidatesPanel'
import { EnrichmentJobsPanel } from '@/features/enrichment/components/EnrichmentJobsPanel'
import { useCompany, useVerifyCompany } from '../hooks'

/**
 * Company detail — the drill-down after a click from the list.
 *
 * Structure:
 *   • PageHeader with breadcrumb-style back button, name and top-line
 *     status chips.
 *   • Two 50/50 info cards (Overview, Location & status).
 *   • Sales-Intelligence card (extended fields).
 *   • NIC codes and Contacts panels — both self-managed.
 */
export function CompanyDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { data: company, isLoading, isError, error } = useCompany(id)
  const verifyMutation = useVerifyCompany(id ?? '')

  if (isLoading) {
    return (
      <div className="space-y-4">
        <LoadingRows count={1} height="h-16" />
        <div className="grid gap-4 md:grid-cols-2">
          <LoadingRows count={1} height="h-64" />
          <LoadingRows count={1} height="h-64" />
        </div>
      </div>
    )
  }
  if (isError) return <ErrorState message={error instanceof Error ? error.message : 'Failed to load company'} />
  if (!company) return null

  return (
    <div className="space-y-6">
      <PageHeader
        breadcrumb={
          <Link to="/companies" className="inline-flex items-center gap-1 hover:text-foreground transition-colors">
            <ArrowLeft className="size-3.5" /> Back to companies
          </Link>
        }
        title={
          <span className="flex items-center gap-3 flex-wrap">
            <span>{company.canonical_name}</span>
            <StatusChip kind="pipeline" value={company.pipeline_state} />
            <StatusChip kind="verification" value={company.verification_status} />
          </span>
        }
        description={
          <span className="flex items-center gap-3 text-muted-foreground">
            <span>Completeness · <span className="font-medium text-foreground tabular-nums">{company.completeness_score}%</span></span>
            {company.website_domain ? (
              <a
                href={`https://${company.website_domain}`}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1 hover:text-primary transition-colors"
              >
                {company.website_domain} <ExternalLink className="size-3" />
              </a>
            ) : null}
          </span>
        }
        actions={
          <>
            <EnrichButton
              companyId={id!}
              lastGoogleEnrich={company.google_last_enriched_at}
              lastWebsiteEnrich={company.website_last_enriched_at}
              lastPhoneEnrich={company.primary_phone_last_enriched_at}
            />
            {company.verification_status !== 'VERIFIED' && (
              <Button
                variant="outline"
                onClick={() => verifyMutation.mutate()}
                disabled={verifyMutation.isPending}
              >
                <CheckCircle2 /> {verifyMutation.isPending ? 'Verifying…' : 'Mark verified'}
              </Button>
            )}
            <Button asChild>
              <Link to={`/companies/${id}/edit`}><Pencil /> Edit</Link>
            </Button>
          </>
        }
      />

      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader className="border-b-0 pb-2">
            <CardTitle className="flex items-center gap-2">
              <Building2 className="size-4 text-muted-foreground" />
              Overview
            </CardTitle>
          </CardHeader>
          <CardContent className="pt-2">
            <DefinitionList
              rows={[
                ['Name', company.canonical_name],
                ['Normalized', <span className="font-mono text-xs" key="n">{company.normalized_name}</span>],
                ['Phone', company.primary_phone_normalized],
                ['Email', company.email],
                ['Website', company.website_domain],
                ['Industry', company.industry],
                ['Size band', company.size_band],
                ['Cluster', company.cluster],
                ['Source', company.source],
              ]}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="border-b-0 pb-2">
            <CardTitle>Location & pipeline</CardTitle>
          </CardHeader>
          <CardContent className="pt-2">
            <DefinitionList
              rows={[
                ['City', company.city],
                ['State', company.state],
                ['Pincode', company.pincode],
                ['District', company.district],
                ['Region', company.region],
                ['Completeness', <span key="c" className="tabular-nums">{company.completeness_score}%</span>],
                ['Created', new Date(company.created_at).toLocaleString()],
                company.verified_by ? ['Verified by', company.verified_by] : undefined,
                company.verified_at ? ['Verified at', new Date(company.verified_at).toLocaleString()] : undefined,
              ].filter(Boolean) as [string, React.ReactNode][]}
            />
          </CardContent>
        </Card>
      </div>

      {(company.turnover || company.employee_count || company.gst_number || company.registration_date ||
        company.source_reference || company.address_line || company.products) ? (
        <Card>
          <CardHeader className="border-b-0 pb-2">
            <CardTitle>Sales intelligence</CardTitle>
          </CardHeader>
          <CardContent className="pt-2">
            <div className="grid gap-x-6 gap-y-3 md:grid-cols-2 lg:grid-cols-4">
              <Field label="Turnover" value={company.turnover ?? '—'} />
              <Field label="Employees" value={company.employee_count ?? '—'} />
              <Field label="GST" value={company.gst_number ?? '—'} />
              <Field label="Registration" value={company.registration_date ?? '—'} />
              <Field label="Source ref" value={<span className="font-mono text-xs">{company.source_reference ?? '—'}</span>} />
              <Field label="Address" value={company.address_line ?? '—'} className="md:col-span-3" />
              <Field label="Products" value={company.products ?? '—'} className="md:col-span-4" />
            </div>
          </CardContent>
        </Card>
      ) : null}

      <CompanyNicSection companyId={id!} />

      <Card>
        <CardHeader className="border-b-0 pb-2">
          <CardTitle>Contacts</CardTitle>
        </CardHeader>
        <CardContent className="pt-2">
          <ContactsPanel companyId={id!} />
        </CardContent>
      </Card>

      {(company.google_place_id || company.website_title || company.social_linkedin ||
        company.primary_phone_status) ? (
        <Card>
          <CardHeader className="border-b-0 pb-2">
            <CardTitle>Enrichment data</CardTitle>
          </CardHeader>
          <CardContent className="pt-2">
            <div className="grid gap-x-6 gap-y-3 md:grid-cols-2 lg:grid-cols-4">
              {company.google_name && <Field label="Google name" value={company.google_name} />}
              {company.google_business_category && <Field label="Business type" value={company.google_business_category} />}
              {company.google_business_status && <Field label="Business status" value={company.google_business_status} />}
              {company.google_maps_url && (
                <Field label="Maps" value={
                  <a href={company.google_maps_url} target="_blank" rel="noopener noreferrer"
                    className="text-primary hover:underline inline-flex items-center gap-1">
                    Open in Maps <ExternalLink className="size-3" />
                  </a>
                } />
              )}
              {company.website_title && <Field label="Website title" value={company.website_title} />}
              {company.website_description && <Field label="Website desc" value={company.website_description} className="md:col-span-3" />}
              {company.website_reachable !== null && company.website_reachable !== undefined && (
                <Field label="Website reachable" value={company.website_reachable ? 'Yes' : 'No'} />
              )}
              {company.social_linkedin && (
                <Field label="LinkedIn" value={
                  <a href={company.social_linkedin} target="_blank" rel="noopener noreferrer" className="text-primary hover:underline truncate">{company.social_linkedin}</a>
                } />
              )}
              {company.social_facebook && (
                <Field label="Facebook" value={
                  <a href={company.social_facebook} target="_blank" rel="noopener noreferrer" className="text-primary hover:underline truncate">{company.social_facebook}</a>
                } />
              )}
              {company.social_x && (
                <Field label="X / Twitter" value={
                  <a href={company.social_x} target="_blank" rel="noopener noreferrer" className="text-primary hover:underline truncate">{company.social_x}</a>
                } />
              )}
              {company.primary_phone_status && <Field label="Phone status" value={company.primary_phone_status} />}
              {company.primary_phone_type && <Field label="Phone type" value={company.primary_phone_type} />}
              {company.primary_phone_carrier && <Field label="Carrier" value={company.primary_phone_carrier} />}
              {company.primary_phone_region && <Field label="Phone region" value={company.primary_phone_region} />}
            </div>
          </CardContent>
        </Card>
      ) : null}

      <CandidatesPanel companyId={id!} />
      <EnrichmentJobsPanel companyId={id!} />

      {company.tags && company.tags.length > 0 && (
        <Card>
          <CardHeader className="border-b-0 pb-2">
            <CardTitle>Tags</CardTitle>
          </CardHeader>
          <CardContent className="pt-2">
            <div className="flex flex-wrap gap-2">
              {company.tags.map((tag) => (
                <span
                  key={tag}
                  className="inline-flex items-center rounded-md bg-muted px-2 py-0.5 text-xs font-medium text-foreground"
                >
                  {tag}
                </span>
              ))}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}

function DefinitionList({ rows }: { rows: [string, React.ReactNode | undefined | null][] }) {
  return (
    <dl className="grid grid-cols-[130px_1fr] gap-x-4 gap-y-2 text-sm">
      {rows.map(([label, value]) => (
        <div key={label} className="contents">
          <dt className="text-xs uppercase tracking-wide text-muted-foreground pt-0.5">{label}</dt>
          <dd className="text-foreground min-w-0 break-words">
            {value === null || value === undefined || value === '' ? (
              <span className="text-muted-foreground">—</span>
            ) : value}
          </dd>
        </div>
      ))}
    </dl>
  )
}

function Field({ label, value, className }: { label: string; value: React.ReactNode; className?: string }) {
  return (
    <div className={className}>
      <div className="text-xs uppercase tracking-wide text-muted-foreground">{label}</div>
      <div className="text-sm mt-0.5 break-words">{value}</div>
    </div>
  )
}

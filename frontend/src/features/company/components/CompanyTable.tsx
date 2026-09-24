import { Button } from '@/components/ui/button'
import { StatusChip } from '@/components/feedback/StatusChip'
import { EmptyState } from '@/components/feedback/EmptyState'
import {
  ArrowDown, ArrowUp, ArrowUpDown, Building2, ChevronDown, ChevronRight, Eye, Pencil,
  ExternalLink, Globe, Link2, Mail, MapPin, Phone, User,
} from 'lucide-react'
import { Fragment, useState } from 'react'
import { Link } from 'react-router'
import type { Company } from '../types'

interface Props {
  companies: Company[]
  onSort: (field: string) => void
  sortField: string
  sortDir: string
}

/**
 * Columns shown in the collapsed row — the fields analysts scan a list by:
 *   1. Company (name + pipeline/verification status)  2. Location
 *   3. Industry / NIC                                  4. Phone
 *   5. Website                                          6. Status
 *
 * Everything else (contact details, full address, business/financial
 * fields, enrichment data) is available by expanding the row via the
 * chevron in the first column, grouped into the same sub-sections the
 * company detail page uses.
 */
export function CompanyTable({ companies, onSort, sortField, sortDir }: Props) {
  const [expanded, setExpanded] = useState<Set<string>>(new Set())

  function toggle(id: string) {
    setExpanded((current) => {
      const next = new Set(current)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

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
    <div className="overflow-hidden rounded-lg border border-border/70 bg-card shadow-sm">
      <div className="overflow-x-auto">
        <table className="w-full text-sm" data-tabular="true">
          <thead className="bg-muted/40 sticky top-0">
            <tr className="border-b border-border/70">
              <th scope="col" className="w-9 px-2 py-2.5">
                <span className="sr-only">Expand</span>
              </th>
              <SortableTh label="Company" sortKey="canonicalName" {...{ sortField, sortDir, onSort }} />
              <Th>Location</Th>
              <Th>Industry / NIC</Th>
              <Th>Phone</Th>
              <Th>Website</Th>
              <Th>Status</Th>
              <th scope="col" className="px-3 py-2.5 text-right text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border/70">
            {companies.map((c) => {
              const isExpanded = expanded.has(c.id)
              return (
                <Fragment key={c.id}>
                  <tr className="hover:bg-muted/30 transition-colors align-top">
                    <td className="px-2 py-2.5">
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        onClick={() => toggle(c.id)}
                        aria-expanded={isExpanded}
                        aria-label={isExpanded ? `Collapse ${c.canonical_name}` : `Expand ${c.canonical_name}`}
                      >
                        {isExpanded ? <ChevronDown className="size-4" /> : <ChevronRight className="size-4" />}
                      </Button>
                    </td>
                    {/* Company */}
                    <td className="px-3 py-2.5 max-w-[220px]">
                      <Link
                        to={`/companies/${c.id}`}
                        className="font-medium text-foreground hover:text-primary transition-colors block truncate"
                      >
                        {c.canonical_name}
                      </Link>
                    </td>
                    {/* Location */}
                    <td className="px-3 py-2.5 whitespace-nowrap">
                      <div>{c.city ?? '—'}</div>
                      <div className="text-[11px] text-muted-foreground">{c.district ?? c.state ?? ''}</div>
                    </td>
                    {/* Industry / NIC */}
                    <td className="px-3 py-2.5 max-w-[220px]">
                      <div className="truncate">{c.industry ?? '—'}</div>
                      {c.nic_codes && c.nic_codes.length > 0 ? (
                        <div className="mt-1 flex flex-wrap gap-1">
                          {c.nic_codes.slice(0, 2).map((n) => (
                            <span
                              key={n.code}
                              title={n.description}
                              className={
                                'inline-flex items-center gap-0.5 rounded-md px-1.5 py-0.5 text-[11px] font-medium ' +
                                (n.primary
                                  ? 'bg-primary/10 text-primary border border-primary/20'
                                  : 'bg-muted text-foreground border border-border')
                              }
                            >
                              {n.primary ? <span className="text-amber-500">★</span> : null}
                              {n.code}
                            </span>
                          ))}
                          {c.nic_codes.length > 2 ? (
                            <span className="text-[11px] text-muted-foreground">+{c.nic_codes.length - 2}</span>
                          ) : null}
                        </div>
                      ) : null}
                    </td>
                    {/* Phone */}
                    <td className="px-3 py-2.5 whitespace-nowrap">
                      {c.primary_contact_phone ? (
                        <a href={`tel:${c.primary_contact_phone}`} className="inline-flex items-center gap-1 text-foreground hover:text-primary">
                          <Phone className="size-3 text-muted-foreground" />
                          {c.primary_contact_phone}
                        </a>
                      ) : c.primary_phone_normalized ? (
                        <span className="inline-flex items-center gap-1 text-muted-foreground">
                          <Phone className="size-3" />
                          {c.primary_phone_normalized}
                        </span>
                      ) : '—'}
                    </td>
                    {/* Website */}
                    <td className="px-3 py-2.5 max-w-[180px]">
                      {c.website_domain ? (
                        <a
                          href={`https://${c.website_domain}`}
                          target="_blank" rel="noopener noreferrer"
                          className="inline-flex items-center gap-1 text-foreground hover:text-primary truncate"
                        >
                          <span className="truncate">{c.website_domain}</span>
                          <ExternalLink className="size-3 shrink-0 opacity-60" />
                        </a>
                      ) : '—'}
                    </td>
                    {/* Status */}
                    <td className="px-3 py-2.5">
                      <div className="flex flex-col gap-1 items-start">
                        <StatusChip kind="pipeline" value={c.pipeline_state} />
                        <StatusChip kind="verification" value={c.verification_status} />
                      </div>
                    </td>
                    {/* Actions */}
                    <td className="px-3 py-2.5">
                      <div className="flex items-center justify-end gap-0.5">
                        <Button variant="ghost" size="icon-sm" asChild>
                          <Link to={`/companies/${c.id}`} aria-label="View">
                            <Eye className="size-4" />
                          </Link>
                        </Button>
                        <Button variant="ghost" size="icon-sm" asChild>
                          <Link to={`/companies/${c.id}/edit`} aria-label="Edit">
                            <Pencil className="size-4" />
                          </Link>
                        </Button>
                      </div>
                    </td>
                  </tr>
                  {isExpanded && (
                    <tr className="bg-muted/20">
                      <td colSpan={8} className="px-4 py-4">
                        <CompanyDetailsPanel company={c} />
                      </td>
                    </tr>
                  )}
                </Fragment>
              )
            })}
          </tbody>
        </table>
      </div>
    </div>
  )
}

function CompanyDetailsPanel({ company: c }: { company: Company }) {
  return (
    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
      <DetailSection title="Contact Details">
        <DetailRow label="Contact person" value={c.primary_contact_name} icon={<User className="size-3" />} />
        <DetailRow label="Role" value={c.primary_contact_role} />
        <DetailRow label="Contact phone" value={c.primary_contact_phone} />
        <DetailRow label="Company phone" value={c.primary_phone_normalized} />
        <DetailRow
          label="Email"
          value={c.email}
          icon={<Mail className="size-3" />}
          href={c.email ? `mailto:${c.email}` : undefined}
        />
      </DetailSection>

      <DetailSection title="Location Details">
        <DetailRow label="Address" value={c.address_line} icon={<MapPin className="size-3" />} />
        <DetailRow label="Region" value={c.region} />
        <DetailRow label="State" value={c.state} />
        <DetailRow label="Pincode" value={c.pincode} />
        <DetailRow label="Cluster" value={c.cluster} />
      </DetailSection>

      <DetailSection title="Business Info">
        <DetailRow label="Products" value={c.products} />
        <DetailRow label="Turnover" value={c.turnover != null ? formatCurrency(c.turnover) : null} />
        <DetailRow label="GST number" value={c.gst_number} />
        <DetailRow label="Employees" value={c.employee_count?.toLocaleString()} />
        <DetailRow label="Size band" value={c.size_band} />
        <DetailRow label="Registration date" value={c.registration_date} />
        <DetailRow label="Source" value={c.source} />
        <DetailRow label="Completeness" value={`${c.completeness_score}%`} />
        {c.nic_codes && c.nic_codes.length > 0 && (
          <div className="pt-1">
            <div className="text-[11px] font-medium uppercase tracking-wide text-muted-foreground mb-1">NIC codes</div>
            <div className="flex flex-wrap gap-1">
              {c.nic_codes.map((n) => (
                <span
                  key={n.code}
                  title={n.description}
                  className={
                    'inline-flex items-center gap-0.5 rounded-md px-1.5 py-0.5 text-[11px] font-medium ' +
                    (n.primary
                      ? 'bg-primary/10 text-primary border border-primary/20'
                      : 'bg-muted text-foreground border border-border')
                  }
                >
                  {n.primary ? <span className="text-amber-500">★</span> : null}
                  {n.code}
                </span>
              ))}
            </div>
          </div>
        )}
      </DetailSection>

      <DetailSection title="Enrichment & Social">
        <DetailRow label="Google category" value={c.google_business_category} />
        <DetailRow label="Website title" value={c.website_title} />
        <DetailRow label="Phone status" value={c.primary_phone_status} />
        <DetailRow label="Phone carrier" value={c.primary_phone_carrier} />
        <div className="flex flex-wrap gap-2 pt-1">
          <SocialLink href={c.social_linkedin} icon={<Link2 className="size-3.5" />} label="LinkedIn" />
          <SocialLink href={c.social_facebook} icon={<Link2 className="size-3.5" />} label="Facebook" />
          <SocialLink href={c.social_instagram} icon={<Link2 className="size-3.5" />} label="Instagram" />
          <SocialLink href={c.social_youtube} icon={<Link2 className="size-3.5" />} label="YouTube" />
          <SocialLink href={c.google_maps_url} icon={<Globe className="size-3.5" />} label="Maps" />
        </div>
      </DetailSection>
    </div>
  )
}

function DetailSection({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="rounded-lg border border-border/60 bg-card p-3">
      <h4 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground mb-2">{title}</h4>
      <div className="space-y-1.5 text-sm">{children}</div>
    </div>
  )
}

function DetailRow({
  label, value, icon, href,
}: {
  label: string
  value: string | number | null | undefined
  icon?: React.ReactNode
  href?: string
}) {
  if (value === null || value === undefined || value === '') return null
  return (
    <div className="flex items-start justify-between gap-2">
      <span className="text-muted-foreground shrink-0">{label}</span>
      {href ? (
        <a href={href} className="inline-flex items-center gap-1 text-right text-foreground hover:text-primary truncate">
          {icon}
          {value}
        </a>
      ) : (
        <span className="inline-flex items-center gap-1 text-right font-medium truncate">
          {icon}
          {value}
        </span>
      )}
    </div>
  )
}

function SocialLink({ href, icon, label }: { href?: string | null; icon: React.ReactNode; label: string }) {
  if (!href) return null
  return (
    <a
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      className="inline-flex items-center gap-1 rounded-md border border-border bg-muted px-2 py-1 text-xs text-foreground hover:text-primary"
    >
      {icon}
      {label}
    </a>
  )
}

function Th({ children }: { children: React.ReactNode }) {
  return (
    <th scope="col" className="px-3 py-2.5 text-left text-xs font-semibold uppercase tracking-wide text-muted-foreground whitespace-nowrap">
      {children}
    </th>
  )
}

function SortableTh({
  label, sortKey, sortField, sortDir, onSort,
}: {
  label: string
  sortKey: string
  sortField: string
  sortDir: string
  onSort: (field: string) => void
}) {
  const active = sortField === sortKey
  return (
    <th scope="col" className="px-3 py-2.5 text-left text-xs font-semibold uppercase tracking-wide text-muted-foreground whitespace-nowrap">
      <button
        type="button"
        onClick={() => onSort(sortKey)}
        className="inline-flex items-center gap-1 hover:text-foreground transition-colors"
      >
        <span>{label}</span>
        {active ? (
          sortDir === 'asc' ? <ArrowUp className="size-3" /> : <ArrowDown className="size-3" />
        ) : (
          <ArrowUpDown className="size-3 opacity-40" />
        )}
      </button>
    </th>
  )
}

function formatCurrency(v: string | number): string {
  const n = typeof v === 'string' ? Number(v) : v
  if (!Number.isFinite(n)) return String(v)
  if (Math.abs(n) >= 10_000_000) return `₹${(n / 10_000_000).toFixed(2)}Cr`
  if (Math.abs(n) >= 100_000)    return `₹${(n / 100_000).toFixed(2)}L`
  if (Math.abs(n) >= 1_000)      return `₹${(n / 1_000).toFixed(1)}K`
  return `₹${n.toLocaleString('en-IN')}`
}

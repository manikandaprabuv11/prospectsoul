import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import { useContactRoles } from '@/features/contactrole/hooks'
import { useNicPrimary } from '@/features/nic/hooks'
import { RotateCw, SlidersHorizontal } from 'lucide-react'
import { useState } from 'react'
import type { CompanyFilters as Filters } from '../types'

interface Props {
  filters: Filters
  onChange: (filters: Filters) => void
}

const PIPELINE_STATES = ['IMPORTED', 'TRIAGE', 'RESEARCH', 'QUALIFICATION', 'READY', 'EXPORTED', 'DISQUALIFIED', 'ARCHIVED']
const VERIFICATION_STATUSES = ['UNVERIFIED', 'VERIFIED', 'INVALIDATED']

/**
 * Filter panel — primary controls always visible, secondary facets
 * behind a collapsible "Advanced" section so the panel stays quiet
 * until the user needs a deeper filter.
 */
export function CompanyFilters({ filters, onChange }: Props) {
  const nicPrimary = useNicPrimary()
  const contactRoles = useContactRoles(false)
  const [advanced, setAdvanced] = useState(false)

  const activeCount = countActive(filters)

  const clear = () =>
    onChange({ page: 0, size: 25, sort: 'createdAt', sort_dir: 'desc' })

  return (
    <div className="space-y-4">
      {/* Row 1 — search + primary filters */}
      <div className="grid gap-3 md:grid-cols-[minmax(0,1fr)_repeat(2,minmax(0,200px))_auto]">
        <div className="min-w-0">
          <Label htmlFor="filter-search">Search</Label>
          <Input
            id="filter-search"
            className="mt-1"
            placeholder="Name, phone, email, domain, contact…"
            value={filters.q ?? ''}
            onChange={(e) => onChange({ ...filters, q: e.target.value, page: 0 })}
          />
        </div>
        <div className="min-w-0">
          <Label htmlFor="filter-state">Pipeline</Label>
          <Select
            id="filter-state"
            className="mt-1"
            value={filters.pipeline_state ?? ''}
            onChange={(e) => onChange({ ...filters, pipeline_state: e.target.value || undefined, page: 0 })}
          >
            <option value="">All states</option>
            {PIPELINE_STATES.map((s) => <option key={s} value={s}>{titleCase(s)}</option>)}
          </Select>
        </div>
        <div className="min-w-0">
          <Label htmlFor="filter-verification">Verification</Label>
          <Select
            id="filter-verification"
            className="mt-1"
            value={filters.verification_status ?? ''}
            onChange={(e) => onChange({ ...filters, verification_status: e.target.value || undefined, page: 0 })}
          >
            <option value="">Any</option>
            {VERIFICATION_STATUSES.map((s) => <option key={s} value={s}>{titleCase(s)}</option>)}
          </Select>
        </div>
        <div className="flex items-end gap-2">
          <Button
            variant="outline"
            size="sm"
            className="h-9"
            onClick={() => setAdvanced((v) => !v)}
            aria-expanded={advanced}
            aria-controls="advanced-filters"
          >
            <SlidersHorizontal /> Advanced
            {activeCount > 0 && (
              <span className="ml-1 inline-flex h-5 min-w-5 items-center justify-center rounded-full bg-primary/15 px-1 text-[10px] font-semibold text-primary">
                {activeCount}
              </span>
            )}
          </Button>
          {activeCount > 0 && (
            <Button variant="ghost" size="sm" className="h-9" onClick={clear}>
              <RotateCw /> Clear
            </Button>
          )}
        </div>
      </div>

      {advanced && (
        <div id="advanced-filters" className="grid gap-3 rounded-md border border-border/60 bg-muted/30 p-3 md:grid-cols-4">
          <FilterField label="NIC (with sub-codes)">
            <Select
              value={filters.nic_parent_id ?? ''}
              onChange={(e) => onChange({ ...filters, nic_parent_id: e.target.value || undefined, page: 0 })}
            >
              <option value="">Any NIC</option>
              {nicPrimary.data?.map((n) => (
                <option key={n.id} value={n.id}>{n.code} · {n.description}</option>
              ))}
            </Select>
          </FilterField>
          <FilterField label="Region">
            <Input value={filters.region ?? ''} onChange={(e) => onChange({ ...filters, region: e.target.value || undefined, page: 0 })} placeholder="e.g. South — TN" />
          </FilterField>
          <FilterField label="District">
            <Input value={filters.district ?? ''} onChange={(e) => onChange({ ...filters, district: e.target.value || undefined, page: 0 })} placeholder="e.g. Kanchipuram" />
          </FilterField>
          <FilterField label="Pincode">
            <Input value={filters.pincode ?? ''} onChange={(e) => onChange({ ...filters, pincode: e.target.value || undefined, page: 0 })} maxLength={6} placeholder="6 digits" />
          </FilterField>
          <FilterField label="City">
            <Input value={filters.city ?? ''} onChange={(e) => onChange({ ...filters, city: e.target.value || undefined, page: 0 })} />
          </FilterField>
          <FilterField label="State">
            <Input value={filters.state ?? ''} onChange={(e) => onChange({ ...filters, state: e.target.value || undefined, page: 0 })} />
          </FilterField>
          <FilterField label="Turnover">
            <div className="flex gap-2">
              <Input type="number" placeholder="min" value={filters.turnover_min ?? ''} onChange={(e) => onChange({ ...filters, turnover_min: e.target.value || undefined, page: 0 })} />
              <Input type="number" placeholder="max" value={filters.turnover_max ?? ''} onChange={(e) => onChange({ ...filters, turnover_max: e.target.value || undefined, page: 0 })} />
            </div>
          </FilterField>
          <FilterField label="Employees">
            <div className="flex gap-2">
              <Input type="number" placeholder="min" value={filters.employee_min ?? ''} onChange={(e) => onChange({ ...filters, employee_min: e.target.value || undefined, page: 0 })} />
              <Input type="number" placeholder="max" value={filters.employee_max ?? ''} onChange={(e) => onChange({ ...filters, employee_max: e.target.value || undefined, page: 0 })} />
            </div>
          </FilterField>
          <FilterField label="GST">
            <Select
              value={filters.gst_present === undefined ? '' : String(filters.gst_present)}
              onChange={(e) => onChange({
                ...filters,
                gst_present: e.target.value === '' ? undefined : e.target.value === 'true',
                page: 0,
              })}
            >
              <option value="">Any</option>
              <option value="true">GST present</option>
              <option value="false">GST missing</option>
            </Select>
          </FilterField>
          <FilterField label="Has contact role">
            <Select
              value={filters.has_contact_role_id ?? ''}
              onChange={(e) => onChange({ ...filters, has_contact_role_id: e.target.value || undefined, page: 0 })}
            >
              <option value="">Any</option>
              {contactRoles.data?.map((r) => (
                <option key={r.id} value={r.id}>{r.label}</option>
              ))}
            </Select>
          </FilterField>

          {filters.nic_parent_id ? (
            <>
              <FilterField label="Sub-codes">
                <label className="flex h-9 items-center gap-2 text-sm text-foreground">
                  <input
                    type="checkbox"
                    checked={filters.nic_include_descendants !== false}
                    onChange={(e) => onChange({
                      ...filters,
                      nic_include_descendants: e.target.checked ? undefined : false,
                      page: 0,
                    })}
                  />
                  Include descendants
                </label>
              </FilterField>
              <FilterField label="View">
                <div className="flex gap-1.5">
                  <Button
                    size="sm"
                    variant={filters.view !== 'grouped_by_nic' ? 'default' : 'outline'}
                    onClick={() => onChange({ ...filters, view: undefined })}
                  >Flat</Button>
                  <Button
                    size="sm"
                    variant={filters.view === 'grouped_by_nic' ? 'default' : 'outline'}
                    onClick={() => onChange({ ...filters, view: 'grouped_by_nic' })}
                  >Grouped by NIC</Button>
                </div>
              </FilterField>
            </>
          ) : null}
        </div>
      )}
    </div>
  )
}

function FilterField({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="min-w-0 space-y-1">
      <Label>{label}</Label>
      {children}
    </div>
  )
}

function titleCase(s: string) {
  return s.toLowerCase().split('_').map((w) => w.charAt(0).toUpperCase() + w.slice(1)).join(' ')
}

function countActive(f: Filters) {
  const keys: (keyof Filters)[] = [
    'q','city','state','pipeline_state','verification_status',
    'region','district','pincode','turnover_min','turnover_max',
    'employee_min','employee_max','gst_present','nic_code_id','nic_parent_id',
    'has_contact_role_id',
  ]
  return keys.filter((k) => {
    const v = f[k]
    return v !== undefined && v !== null && v !== ''
  }).length
}

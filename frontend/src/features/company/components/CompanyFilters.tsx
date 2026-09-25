import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import { useContactRoles } from '@/features/contactrole/hooks'
import { useCompanyDefaults } from '@/features/companydefaults/hooks'
import { useNicCode, useNicPrimary, useResolveNicByCode } from '@/features/nic/hooks'
import { Lock, RotateCw, Search, SlidersHorizontal, X } from 'lucide-react'
import { useState } from 'react'
import type { CompanyFilters as Filters } from '../types'

interface Props {
  filters: Filters
  onChange: (filters: Filters) => void
}

const PIPELINE_STATES = ['IMPORTED', 'TRIAGE', 'RESEARCH', 'QUALIFICATION', 'READY', 'EXPORTED', 'DISQUALIFIED', 'ARCHIVED']
const VERIFICATION_STATUSES = ['UNVERIFIED', 'VERIFIED', 'INVALIDATED']

export function CompanyFilters({ filters, onChange }: Props) {
  const nicPrimary = useNicPrimary()
  const contactRoles = useContactRoles(false)
  const [advanced, setAdvanced] = useState(false)

  const selectedNicIds = filters.nic_parent_ids ?? []

  // Label cache for chips — populated as NICs are picked (from the primary
  // dropdown, which already carries code+description, or from a manual-code
  // resolution). Lost on refresh, same as every other filter on this page
  // (state here is component-local, not URL-backed) — falls back to the raw
  // id, which is still a working filter, just a less readable chip.
  const [nicLabels, setNicLabels] = useState<Record<string, string>>({})

  const [nicSelectValue, setNicSelectValue] = useState('')
  const [manualNicCode, setManualNicCode] = useState('')
  const [manualNicError, setManualNicError] = useState<string | null>(null)
  const resolveNic = useResolveNicByCode()

  // Admin-configured default NIC scope (Company Defaults). When active it is
  // ANDed with whatever the analyst picks here (server-side, in
  // CompanyController) — surfaced as a read-only chip so that combination is
  // visible rather than a silent server-side override.
  const companyDefaults = useCompanyDefaults()
  const nicDefaultRow = companyDefaults.data?.find((d) => d.filter_key === 'nic_parent_id' && d.active)
  const configNicId = parseNicDefaultId(nicDefaultRow?.value)
  const configNic = useNicCode(configNicId)

  function addNicParent(id: string, label?: string) {
    if (!id || selectedNicIds.includes(id)) return
    if (label) setNicLabels((prev) => ({ ...prev, [id]: label }))
    onChange({ ...filters, nic_parent_ids: [...selectedNicIds, id], page: 0 })
  }

  function removeNicParent(id: string) {
    const next = selectedNicIds.filter((x) => x !== id)
    onChange({ ...filters, nic_parent_ids: next.length ? next : undefined, page: 0 })
  }

  function handleDropdownNicChange(value: string) {
    setNicSelectValue('')
    if (!value) return
    const nic = nicPrimary.data?.find((n) => n.id === value)
    addNicParent(value, nic ? `${nic.code} · ${nic.description}` : undefined)
  }

  function submitManualNic() {
    const trimmed = manualNicCode.trim()
    if (!trimmed) return
    resolveNic.mutate(trimmed, {
      onSuccess: (nic) => {
        setManualNicError(null)
        setManualNicCode('')
        addNicParent(nic.id, `${nic.code} · ${nic.description}`)
      },
      onError: (err) => {
        setManualNicError(err instanceof Error ? err.message : `No NIC found for '${trimmed}'`)
      },
    })
  }

  const activeCount = countActive(filters)

  const clear = () =>
    onChange({ page: 0, size: 25, sort: 'createdAt', sort_dir: 'desc' })

  return (
    <div className="space-y-4">
      {/* Row 1 — search + primary filters */}
      <div className="grid gap-3 md:grid-cols-[minmax(0,1fr)_repeat(2,minmax(0,200px))_auto]">
        <div className="min-w-0">
          <Label htmlFor="filter-search" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Search</Label>
          <div className="relative mt-1">
            <Search className="pointer-events-none absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground/50" />
            <Input
              id="filter-search"
              className="pl-8"
              placeholder="Name, phone, email, domain, contact…"
              value={filters.q ?? ''}
              onChange={(e) => onChange({ ...filters, q: e.target.value, page: 0 })}
            />
          </div>
        </div>
        <div className="min-w-0">
          <Label htmlFor="filter-state" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Pipeline</Label>
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
          <Label htmlFor="filter-verification" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Verification</Label>
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
              <span className="ml-1 inline-flex h-5 min-w-5 items-center justify-center rounded-full bg-primary/15 px-1 text-[10px] font-bold text-primary">
                {activeCount}
              </span>
            )}
          </Button>
          {activeCount > 0 && (
            <Button variant="ghost" size="sm" className="h-9 text-muted-foreground" onClick={clear}>
              <RotateCw /> Clear
            </Button>
          )}
        </div>
      </div>

      {/* Active filter chips */}
      {activeCount > 0 && (
        <div className="flex flex-wrap gap-1.5">
          {filters.q && <FilterChip label={`Search: "${filters.q}"`} onRemove={() => onChange({ ...filters, q: undefined, page: 0 })} />}
          {filters.pipeline_state && <FilterChip label={`Pipeline: ${titleCase(filters.pipeline_state)}`} onRemove={() => onChange({ ...filters, pipeline_state: undefined, page: 0 })} />}
          {filters.verification_status && <FilterChip label={`Verification: ${titleCase(filters.verification_status)}`} onRemove={() => onChange({ ...filters, verification_status: undefined, page: 0 })} />}
          {filters.city && <FilterChip label={`City: ${filters.city}`} onRemove={() => onChange({ ...filters, city: undefined, page: 0 })} />}
          {filters.state && <FilterChip label={`State: ${filters.state}`} onRemove={() => onChange({ ...filters, state: undefined, page: 0 })} />}
          {filters.region && <FilterChip label={`Region: ${filters.region}`} onRemove={() => onChange({ ...filters, region: undefined, page: 0 })} />}
        </div>
      )}

      {advanced && (
        <div id="advanced-filters" className="grid gap-3 rounded-xl border border-border bg-surface-1/50 p-4 md:grid-cols-4 animate-slide-up">
          <FilterField label="NIC (with sub-codes)" className="md:col-span-2">
            <div className="flex gap-2">
              <Select
                className="flex-1"
                value={nicSelectValue}
                onChange={(e) => handleDropdownNicChange(e.target.value)}
              >
                <option value="">Add a NIC…</option>
                {nicPrimary.data
                  ?.filter((n) => !selectedNicIds.includes(n.id))
                  .map((n) => (
                    <option key={n.id} value={n.id}>{n.code} · {n.description}</option>
                  ))}
              </Select>
              <Input
                className="w-32"
                value={manualNicCode}
                onChange={(e) => { setManualNicCode(e.target.value); setManualNicError(null) }}
                onBlur={submitManualNic}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') { e.preventDefault(); submitManualNic() }
                }}
                placeholder="or type code"
                aria-invalid={manualNicError ? true : undefined}
                aria-describedby={manualNicError ? 'nic-manual-error' : undefined}
              />
            </div>
            {manualNicError ? (
              <p id="nic-manual-error" className="mt-1 text-xs text-destructive">{manualNicError}</p>
            ) : null}
            {(selectedNicIds.length > 0 || configNicId) && (
              <div className="mt-2 flex flex-wrap gap-1.5">
                {configNicId && (
                  <span
                    className="inline-flex items-center gap-1 rounded-lg bg-muted px-2.5 py-1 text-xs font-medium text-muted-foreground"
                    title="Set in Companies · default filters — always ANDed with your selection above"
                  >
                    <Lock className="size-3" />
                    Config: {configNic.data ? `${configNic.data.code} · ${configNic.data.description}` : configNicId}
                  </span>
                )}
                {selectedNicIds.map((id) => (
                  <FilterChip
                    key={id}
                    label={nicLabels[id] ?? id}
                    onRemove={() => removeNicParent(id)}
                  />
                ))}
              </div>
            )}
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

          {selectedNicIds.length > 0 ? (
            <>
              <FilterField label="Sub-codes">
                <label className="flex h-9 items-center gap-2 text-sm text-foreground cursor-pointer">
                  <input
                    type="checkbox"
                    checked={filters.nic_include_descendants !== false}
                    onChange={(e) => onChange({
                      ...filters,
                      nic_include_descendants: e.target.checked ? undefined : false,
                      page: 0,
                    })}
                    className="rounded"
                  />
                  Include descendants
                </label>
              </FilterField>
              {/* Grouped-by-NIC is inherently single-parent (docs 21 §4.3) — only offered with exactly one NIC selected. */}
              {selectedNicIds.length === 1 && (
              <FilterField label="View">
                <div className="flex gap-1">
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
              )}
            </>
          ) : null}
        </div>
      )}
    </div>
  )
}

function FilterField({ label, children, className }: { label: string; children: React.ReactNode; className?: string }) {
  return (
    <div className={`min-w-0 space-y-1 ${className ?? ''}`}>
      <Label className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">{label}</Label>
      {children}
    </div>
  )
}

function FilterChip({ label, onRemove }: { label: string; onRemove: () => void }) {
  return (
    <span className="inline-flex items-center gap-1 rounded-lg bg-primary/8 px-2.5 py-1 text-xs font-medium text-primary">
      {label}
      <button type="button" onClick={onRemove} className="hover:text-primary/70 transition-colors" aria-label={`Remove filter: ${label}`}>
        <X className="size-3" />
      </button>
    </span>
  )
}

function titleCase(s: string) {
  return s.toLowerCase().split('_').map((w) => w.charAt(0).toUpperCase() + w.slice(1)).join(' ')
}

/** The default filter's `value` is stored as raw JSON (e.g. `"<uuid>"`) — parse it defensively. */
function parseNicDefaultId(raw: string | null | undefined): string | undefined {
  if (!raw) return undefined
  try {
    const parsed: unknown = JSON.parse(raw)
    return typeof parsed === 'string' && parsed ? parsed : undefined
  } catch {
    return undefined
  }
}

function countActive(f: Filters) {
  const keys: (keyof Filters)[] = [
    'q','city','state','pipeline_state','verification_status',
    'region','district','pincode','turnover_min','turnover_max',
    'employee_min','employee_max','gst_present','nic_code_id',
    'has_contact_role_id',
  ]
  const scalarCount = keys.filter((k) => {
    const v = f[k]
    return v !== undefined && v !== null && v !== ''
  }).length
  return scalarCount + (f.nic_parent_ids?.length ?? 0)
}

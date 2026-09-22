import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { useContactRoles } from '@/features/contactrole/hooks'
import { useNicPrimary } from '@/features/nic/hooks'
import type { CompanyFilters as Filters } from '../types'

interface Props {
  filters: Filters
  onChange: (filters: Filters) => void
}

const PIPELINE_STATES = ['IMPORTED', 'TRIAGE', 'RESEARCH', 'QUALIFICATION', 'READY', 'EXPORTED', 'DISQUALIFIED', 'ARCHIVED']
const VERIFICATION_STATUSES = ['UNVERIFIED', 'VERIFIED', 'INVALIDATED']

/**
 * Extended filter panel (UI/UX Addendum §4.1). Adds:
 *   - NIC picker (primary-first sort from /nic-codes/primary)
 *   - Region / District / Pincode
 *   - Turnover / employee ranges
 *   - GST presence
 *   - "Has contact with role X"
 *   - View toggle (flat / grouped by NIC)
 */
export function CompanyFilters({ filters, onChange }: Props) {
  const nicPrimary = useNicPrimary()
  const contactRoles = useContactRoles(false)

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-end gap-3">
        <div className="flex-1 min-w-[220px]">
          <Input
            placeholder="Search name / phone / email / domain / contact…"
            value={filters.q ?? ''}
            onChange={(e) => onChange({ ...filters, q: e.target.value, page: 0 })}
          />
        </div>
        <Select
          value={filters.pipeline_state ?? ''}
          onChange={(e) => onChange({ ...filters, pipeline_state: e.target.value || undefined, page: 0 })}
        >
          <option value="">All states</option>
          {PIPELINE_STATES.map((s) => <option key={s} value={s}>{s}</option>)}
        </Select>
        <Select
          value={filters.verification_status ?? ''}
          onChange={(e) => onChange({ ...filters, verification_status: e.target.value || undefined, page: 0 })}
        >
          <option value="">All verification</option>
          {VERIFICATION_STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
        </Select>
        <Button
          variant="outline"
          size="sm"
          onClick={() => onChange({ page: 0, size: 25, sort: 'createdAt', sort_dir: 'desc' })}
        >
          Clear
        </Button>
      </div>

      <div className="grid grid-cols-1 gap-3 md:grid-cols-4">
        <Select
          value={filters.nic_parent_id ?? ''}
          onChange={(e) => onChange({ ...filters, nic_parent_id: e.target.value || undefined, page: 0 })}
        >
          <option value="">Any NIC</option>
          {nicPrimary.data?.map((n) => (
            <option key={n.id} value={n.id}>{n.code} · {n.description}</option>
          ))}
        </Select>
        <Input
          placeholder="Region"
          value={filters.region ?? ''}
          onChange={(e) => onChange({ ...filters, region: e.target.value || undefined, page: 0 })}
        />
        <Input
          placeholder="District"
          value={filters.district ?? ''}
          onChange={(e) => onChange({ ...filters, district: e.target.value || undefined, page: 0 })}
        />
        <Input
          placeholder="Pincode"
          value={filters.pincode ?? ''}
          onChange={(e) => onChange({ ...filters, pincode: e.target.value || undefined, page: 0 })}
          maxLength={6}
        />
        <Input
          placeholder="City"
          value={filters.city ?? ''}
          onChange={(e) => onChange({ ...filters, city: e.target.value || undefined, page: 0 })}
        />
        <Input
          placeholder="State"
          value={filters.state ?? ''}
          onChange={(e) => onChange({ ...filters, state: e.target.value || undefined, page: 0 })}
        />
        <Input
          type="number"
          placeholder="Turnover min"
          value={filters.turnover_min ?? ''}
          onChange={(e) => onChange({ ...filters, turnover_min: e.target.value || undefined, page: 0 })}
        />
        <Input
          type="number"
          placeholder="Turnover max"
          value={filters.turnover_max ?? ''}
          onChange={(e) => onChange({ ...filters, turnover_max: e.target.value || undefined, page: 0 })}
        />
        <Input
          type="number"
          placeholder="Employees min"
          value={filters.employee_min ?? ''}
          onChange={(e) => onChange({ ...filters, employee_min: e.target.value || undefined, page: 0 })}
        />
        <Input
          type="number"
          placeholder="Employees max"
          value={filters.employee_max ?? ''}
          onChange={(e) => onChange({ ...filters, employee_max: e.target.value || undefined, page: 0 })}
        />
        <Select
          value={filters.gst_present === undefined ? '' : String(filters.gst_present)}
          onChange={(e) => onChange({
            ...filters,
            gst_present: e.target.value === '' ? undefined : e.target.value === 'true',
            page: 0,
          })}
        >
          <option value="">GST · any</option>
          <option value="true">GST present</option>
          <option value="false">GST missing</option>
        </Select>
        <Select
          value={filters.has_contact_role_id ?? ''}
          onChange={(e) => onChange({ ...filters, has_contact_role_id: e.target.value || undefined, page: 0 })}
        >
          <option value="">Has contact · any</option>
          {contactRoles.data?.map((r) => (
            <option key={r.id} value={r.id}>Has {r.label}</option>
          ))}
        </Select>
      </div>

      {filters.nic_parent_id ? (
        <div className="flex items-center gap-4 text-sm">
          <label className="flex items-center gap-2">
            <input
              type="checkbox"
              checked={filters.nic_include_descendants !== false}
              onChange={(e) => onChange({
                ...filters,
                nic_include_descendants: e.target.checked ? undefined : false,
                page: 0,
              })}
            />
            Include all sub-codes
          </label>
          <div className="flex items-center gap-2">
            <span className="text-muted-foreground">View:</span>
            <Button
              size="sm"
              variant={filters.view !== 'grouped_by_nic' ? 'default' : 'outline'}
              onClick={() => onChange({ ...filters, view: undefined })}
            >
              Flat
            </Button>
            <Button
              size="sm"
              variant={filters.view === 'grouped_by_nic' ? 'default' : 'outline'}
              onClick={() => onChange({ ...filters, view: 'grouped_by_nic' })}
            >
              Grouped by NIC
            </Button>
          </div>
        </div>
      ) : null}
    </div>
  )
}

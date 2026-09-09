import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import type { CompanyFilters as Filters } from '../types'

interface Props {
  filters: Filters
  onChange: (filters: Filters) => void
}

const PIPELINE_STATES = ['', 'IMPORTED', 'TRIAGE', 'RESEARCH', 'QUALIFICATION', 'READY', 'EXPORTED', 'DISQUALIFIED', 'ARCHIVED']
const VERIFICATION_STATUSES = ['', 'UNVERIFIED', 'VERIFIED', 'INVALIDATED']

export function CompanyFilters({ filters, onChange }: Props) {
  return (
    <div className="flex flex-wrap items-end gap-3">
      <div className="flex-1">
        <Input
          placeholder="Search companies..."
          value={filters.q ?? ''}
          onChange={(e) => onChange({ ...filters, q: e.target.value, page: 0 })}
        />
      </div>
      <Select
        value={filters.pipeline_state ?? ''}
        onChange={(e) => onChange({ ...filters, pipeline_state: e.target.value || undefined, page: 0 })}
      >
        <option value="">All States</option>
        {PIPELINE_STATES.filter(Boolean).map((s) => (
          <option key={s} value={s}>{s}</option>
        ))}
      </Select>
      <Select
        value={filters.verification_status ?? ''}
        onChange={(e) => onChange({ ...filters, verification_status: e.target.value || undefined, page: 0 })}
      >
        <option value="">All Verification</option>
        {VERIFICATION_STATUSES.filter(Boolean).map((s) => (
          <option key={s} value={s}>{s}</option>
        ))}
      </Select>
      <Input
        placeholder="City"
        className="w-32"
        value={filters.city ?? ''}
        onChange={(e) => onChange({ ...filters, city: e.target.value || undefined, page: 0 })}
      />
      <Input
        placeholder="State"
        className="w-32"
        value={filters.state ?? ''}
        onChange={(e) => onChange({ ...filters, state: e.target.value || undefined, page: 0 })}
      />
      <Button
        variant="outline"
        size="sm"
        onClick={() => onChange({ page: 0, size: 25, sort: 'createdAt', sort_dir: 'desc' })}
      >
        Clear
      </Button>
    </div>
  )
}

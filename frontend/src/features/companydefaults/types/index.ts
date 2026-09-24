export type FilterOperator =
  | 'eq' | 'ne' | 'gte' | 'lte' | 'gt' | 'lt'
  | 'between' | 'in' | 'is_present' | 'is_missing'

export interface CompanyDefaultFilter {
  id: string
  filter_key: string
  label: string
  operator: FilterOperator
  value: string | null   // raw JSON string as stored in the DB
  active: boolean
  sort_order: number
  created_by: string | null
  created_at: string
  updated_by: string | null
  updated_at: string
}

export interface CompanyDefaultFilterUpsertRequest {
  filter_key: string
  label: string
  operator: FilterOperator
  value?: string | null
  active?: boolean
  sort_order?: number
}

export const FILTER_KEY_OPTIONS: { key: string; label: string; operator: FilterOperator; example: string }[] = [
  { key: 'employee_min',         label: 'Minimum employees',         operator: 'gte', example: '10' },
  { key: 'employee_max',         label: 'Maximum employees',         operator: 'lte', example: '500' },
  { key: 'turnover_min',         label: 'Minimum turnover (INR)',    operator: 'gte', example: '10000000' },
  { key: 'turnover_max',         label: 'Maximum turnover (INR)',    operator: 'lte', example: '5000000000' },
  { key: 'gst_present',          label: 'GST present',               operator: 'eq',  example: 'true' },
  { key: 'pipeline_state',       label: 'Pipeline state',            operator: 'eq',  example: '"READY"' },
  { key: 'verification_status',  label: 'Verification status',       operator: 'eq',  example: '"VERIFIED"' },
  { key: 'region',               label: 'Region',                    operator: 'eq',  example: '"South — TN"' },
  { key: 'district',             label: 'District',                  operator: 'eq',  example: '"Coimbatore"' },
  { key: 'pincode',              label: 'Pincode',                   operator: 'eq',  example: '"641006"' },
  { key: 'nic_parent_id',        label: 'NIC (with sub-codes)',      operator: 'eq',  example: '"<uuid>"' },
  { key: 'has_nic_primary',      label: 'Has primary NIC',           operator: 'eq',  example: 'true' },
]

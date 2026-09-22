export type IndustryType = 'Service' | 'Manufacturing' | 'Unknown'

export interface NicCodeResponse {
  id: string
  nic_data_id: number | null
  code: string
  description: string
  industry_type: IndustryType
  level: number
  parent_id: string | null
  is_primary: boolean
  active: boolean
  created_by: string | null
  created_at: string
  updated_by: string | null
  updated_at: string
}

export interface NicTreeNodeResponse {
  id: string
  code: string
  description: string
  industry_type: IndustryType
  level: number
  is_primary: boolean
  active: boolean
  child_count: number
  children: NicTreeNodeResponse[]
}

export interface NicImportResultResponse {
  rows_read: number
  created: number
  updated: number
  unresolved_parents: number
  rejected: number
  rejected_reasons: string[]
}

export interface NicCodeCreateRequest {
  code: string
  description: string
  industry_type: IndustryType
  parent_id?: string | null
  is_primary?: boolean
}

export interface NicCodeUpdateRequest {
  description?: string
  industry_type?: IndustryType
  parent_id?: string | null
  is_primary?: boolean
  active?: boolean
  force_deactivate?: boolean
}

export interface NicListFilters {
  q?: string
  level?: number
  industry_type?: IndustryType
  is_primary?: boolean
  active?: boolean
  page?: number
  size?: number
  sort?: string
  sort_dir?: 'asc' | 'desc'
}

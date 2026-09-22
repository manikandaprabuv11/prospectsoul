export interface ContactRoleResponse {
  id: string
  key: string
  label: string
  sort_order: number
  active: boolean
  usage_count: number
  created_at: string
  updated_at: string
}

export interface ContactRoleCreateRequest {
  key: string
  label: string
  sort_order?: number | null
}

export interface ContactRoleUpdateRequest {
  label?: string
  sort_order?: number
  active?: boolean
}

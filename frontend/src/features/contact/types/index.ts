export interface ContactResponse {
  id: string
  company_id: string
  name: string
  designation: string | null
  phone: string | null
  email: string | null
  role_id: string | null
  role_key: string | null
  role_label: string | null
  is_primary: boolean
  is_md_owner: boolean
  association_start: string | null
  association_end: string | null
  verification_status: string
  created_by: string | null
  created_at: string
  updated_by: string | null
  updated_at: string
}

export interface ContactCreateRequest {
  name: string
  designation?: string | null
  phone?: string | null
  email?: string | null
  role_id: string
  is_primary?: boolean
  association_start?: string | null
  association_end?: string | null
}

export interface ContactUpdateRequest {
  name?: string
  designation?: string | null
  phone?: string | null
  email?: string | null
  role_id?: string
  is_primary?: boolean
  association_start?: string | null
  association_end?: string | null
}

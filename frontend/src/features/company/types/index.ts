export interface Company {
  id: string
  canonical_name: string
  normalized_name: string
  website_domain: string | null
  primary_phone_normalized: string | null
  email: string | null
  city: string | null
  state: string | null
  cluster: string | null
  industry: string | null
  size_band: string | null
  tags: string[] | null
  source: string | null
  pipeline_state: string
  completeness_score: number
  verification_status: string
  verified_by: string | null
  verified_at: string | null
  created_by: string | null
  created_at: string
  updated_by: string | null
  updated_at: string
}

export interface CompanyCreateRequest {
  canonical_name: string
  website_domain?: string
  primary_phone?: string
  email?: string
  city?: string
  state?: string
  cluster?: string
  industry?: string
  size_band?: string
  tags?: string[]
  source?: string
}

export interface CompanyUpdateRequest {
  canonical_name?: string
  website_domain?: string
  primary_phone?: string
  email?: string
  city?: string
  state?: string
  cluster?: string
  industry?: string
  size_band?: string
  tags?: string[]
  source?: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  total_elements: number
  total_pages: number
}

export interface CompanyFilters {
  q?: string
  city?: string
  state?: string
  industry?: string
  cluster?: string
  source?: string
  pipeline_state?: string
  verification_status?: string
  page?: number
  size?: number
  sort?: string
  sort_dir?: string
}

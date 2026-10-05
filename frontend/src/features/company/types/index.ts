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
  pincode?: string | null
  district?: string | null
  address_line?: string | null
  region?: string | null
  products?: string | null
  turnover?: string | number | null
  gst_number?: string | null
  employee_count?: number | null
  registration_date?: string | null
  source_reference?: string | null
  lg_state_code?: number | null
  lg_district_code?: number | null
  primary_nic_code_id?: string | null
  latitude?: string | number | null
  longitude?: string | number | null
  // Enrichment: Google Places
  google_place_id?: string | null
  google_name?: string | null
  google_business_category?: string | null
  google_business_types?: string | null
  google_maps_url?: string | null
  google_lat?: number | null
  google_lng?: number | null
  google_business_status?: string | null
  google_last_enriched_at?: string | null
  // Enrichment: Website
  website_reachable?: boolean | null
  website_title?: string | null
  website_description?: string | null
  website_last_enriched_at?: string | null
  // Enrichment: Social
  social_linkedin?: string | null
  social_facebook?: string | null
  social_x?: string | null
  social_instagram?: string | null
  social_youtube?: string | null
  // v1.2: phone confidence fields (from company_phones)
  primary_phone_confidence?: 'HIGH' | 'MEDIUM' | 'LOW' | null
  primary_phone_designation?: string | null
  additional_phone_count?: number | null
  phones?: CompanyPhone[] | null
  // Enriched by the list endpoint only.
  primary_contact_name?: string | null
  primary_contact_phone?: string | null
  primary_contact_role?: string | null
  nic_codes?: { code: string; description: string; primary: boolean }[] | null
}

export type ConfidenceLevel = 'HIGH' | 'MEDIUM' | 'LOW'
export type NumberSourceType = 'BUSINESS_CARD' | 'FIELD_VISIT' | 'REFERENCE' | 'MANUAL_ENTRY' | 'WEBSITE' | 'GOOGLE_API' | 'LINKEDIN' | 'INDIAMART' | 'IMPORT_DEFAULT'

export interface CompanyPhone {
  id: string
  number_raw: string
  number_normalized: string | null
  phone_type: string
  number_source: NumberSourceType
  confidence: ConfidenceLevel
  confidence_mode: 'AUTO' | 'MANUAL'
  designation: string | null
  is_decision_maker: boolean
  is_primary: boolean
  contact_id: string | null
  contact_name: string | null
  designation_override: string | null
  override_reason: string | null
  enriched_country: string | null
  enriched_region: string | null
  enriched_carrier: string | null
  enriched_line_type: string | null
  enriched_status: string | null
  enriched_dnd: boolean | null
  enriched_at: string | null
  created_at: string
  created_by: string | null
}

export interface CompanyPhoneRequest {
  id?: string | null
  number_raw: string
  number_source: NumberSourceType
  confidence?: ConfidenceLevel | null
  contact_id?: string | null
  designation_override?: string | null
  is_primary?: boolean
}

export interface CompanyCreateRequest {
  canonical_name: string
  website_domain?: string
  email?: string
  city?: string
  state?: string
  cluster?: string
  industry?: string
  size_band?: string
  tags?: string[]
  source?: string
  gst_number?: string
  phones?: CompanyPhoneRequest[]
}

export interface CompanyUpdateRequest {
  canonical_name?: string
  website_domain?: string
  email?: string
  city?: string
  state?: string
  cluster?: string
  industry?: string
  size_band?: string
  tags?: string[]
  source?: string
  gst_number?: string
  phones?: CompanyPhoneRequest[]
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
  region?: string
  district?: string
  pincode?: string
  turnover_min?: string
  turnover_max?: string
  employee_min?: string
  employee_max?: string
  gst_present?: boolean
  nic_code_id?: string
  /** Multi-select NIC parent ids — companies matching ANY of them qualify. */
  nic_parent_ids?: string[]
  nic_include_descendants?: boolean
  has_contact_role_id?: string
  confidence?: ConfidenceLevel
  number_source?: NumberSourceType
  has_decision_maker?: boolean
  view?: 'grouped_by_nic'
  apply_defaults?: boolean
}

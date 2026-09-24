export interface PincodeCentroidResponse {
  pincode: string
  area_name: string
  district: string | null
  state: string | null
  centroid: { lat: string | number; lng: string | number }
}

export interface MapNicCodeRef {
  id: string | null
  code: string
  description: string
  primary: boolean
}

export interface MapCompanyItem {
  id: string
  canonical_name: string
  pipeline_state: string
  lat: string | number
  lng: string | number
  primary_nic_code_id: string | null
  nic_codes: MapNicCodeRef[]
}

export interface MapCompaniesResponse {
  center: { lat: string | number; lng: string | number }
  radius_km: number
  unknown_pincode: boolean
  content: MapCompanyItem[]
  // Full set of NIC code ids the active nic_parent_id filter matched
  // (the parent plus its resolved descendants) — null when no NIC filter
  // is active. Used to show only the matching NIC chips on each card.
  matched_nic_code_ids: string[] | null
}

export interface MapCompaniesFilters {
  nic_parent_id?: string
  nic_include_descendants?: boolean
}



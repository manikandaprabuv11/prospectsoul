export interface PincodeCentroidResponse {
  pincode: string
  area_name: string
  district: string | null
  state: string | null
  centroid: { lat: string | number; lng: string | number }
}

export interface MapCompanyItem {
  id: string
  canonical_name: string
  pipeline_state: string
  lat: string | number
  lng: string | number
  primary_nic_code_id: string | null
}

export interface MapCompaniesResponse {
  center: { lat: string | number; lng: string | number }
  radius_km: number
  unknown_pincode: boolean
  content: MapCompanyItem[]
}

export interface ExternalPlaceResult {
  place_id: string
  name: string
  formatted_address: string
  phone: string | null
  lat: string | number
  lng: string | number
  business_status: string
  types: string[]
}

export interface ExternalPlacesResponse {
  results: ExternalPlaceResult[]
  source: string
  persisted: boolean
  quota_remaining: number
}

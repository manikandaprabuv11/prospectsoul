import { apiClient } from '@/api/client'
import type {
  ExternalPlacesResponse,
  MapCompaniesResponse,
  PincodeCentroidResponse,
} from '../types'

export const locationApi = {
  pincode(pincode: string): Promise<PincodeCentroidResponse> {
    return apiClient.get<PincodeCentroidResponse>(`/map/pincode/${pincode}`)
  },
  companies(pincode: string, radiusKm: number): Promise<MapCompaniesResponse> {
    return apiClient.get<MapCompaniesResponse>(`/map/companies`, {
      query: { pincode, radius_km: radiusKm },
    })
  },
  placesSearch(pincode: string, radiusMeters?: number, keyword?: string): Promise<ExternalPlacesResponse> {
    return apiClient.get<ExternalPlacesResponse>(`/external/places-search`, {
      query: { pincode, radius_m: radiusMeters, keyword },
    })
  },
}

import { apiClient } from '@/api/client'
import type { MapCompaniesFilters, MapCompaniesResponse, PincodeCentroidResponse } from '../types'

export const locationApi = {
  pincode(pincode: string): Promise<PincodeCentroidResponse> {
    return apiClient.get<PincodeCentroidResponse>(`/map/pincode/${pincode}`)
  },
  companies(pincode: string, radiusKm: number, filters?: MapCompaniesFilters): Promise<MapCompaniesResponse> {
    return apiClient.get<MapCompaniesResponse>(`/map/companies`, {
      query: {
        pincode,
        radius_km: radiusKm,
        nic_parent_ids: filters?.nic_parent_ids,
        nic_include_descendants: filters?.nic_include_descendants,
        // Same default as the Companies list — the configured NIC default is
        // always ANDed with the analyst's own selection unless the caller
        // opts out.
        apply_defaults: filters?.apply_defaults ?? true,
      },
    })
  },
}

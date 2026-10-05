import { apiClient } from '@/api/client'
import { env } from '@/constants/env'
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
  async downloadCompanies(
    pincode: string,
    radiusKm: number,
    filters?: MapCompaniesFilters,
    authHeader?: string,
  ): Promise<Blob> {
    const resp = await fetch(`${env.apiBaseUrl}/map/companies/download`, {
      method: 'POST',
      credentials: 'include',
      headers: {
        'content-type': 'application/json',
        ...(authHeader ? { authorization: authHeader } : {}),
      },
      body: JSON.stringify({
        pincode,
        radius_km: radiusKm,
        nic_parent_ids: filters?.nic_parent_ids ?? [],
        nic_include_descendants: filters?.nic_include_descendants,
        apply_defaults: filters?.apply_defaults ?? true,
      }),
    })
    if (!resp.ok) {
      const problem = await resp.json().catch(() => ({}))
      throw new Error(problem.detail ?? `Download failed (${resp.status})`)
    }
    return resp.blob()
  },
}

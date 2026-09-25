import { useQuery } from '@tanstack/react-query'
import { locationApi } from '../api'
import type { MapCompaniesFilters } from '../types'

export function usePincode(pincode: string) {
  return useQuery({
    queryKey: ['pincode', pincode],
    queryFn: () => locationApi.pincode(pincode),
    enabled: !!pincode && /^\d{6}$/.test(pincode),
  })
}

export function useMapCompanies(pincode: string, radiusKm: number, filters?: MapCompaniesFilters) {
  return useQuery({
    queryKey: [
      'map', 'companies', pincode, radiusKm,
      filters?.nic_parent_ids, filters?.nic_include_descendants, filters?.apply_defaults,
    ],
    queryFn: () => locationApi.companies(pincode, radiusKm, filters),
    enabled: !!pincode && /^\d{6}$/.test(pincode),
  })
}


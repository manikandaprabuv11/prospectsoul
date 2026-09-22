import { useQuery } from '@tanstack/react-query'
import { locationApi } from '../api'

export function usePincode(pincode: string) {
  return useQuery({
    queryKey: ['pincode', pincode],
    queryFn: () => locationApi.pincode(pincode),
    enabled: !!pincode && /^\d{6}$/.test(pincode),
  })
}

export function useMapCompanies(pincode: string, radiusKm: number) {
  return useQuery({
    queryKey: ['map', 'companies', pincode, radiusKm],
    queryFn: () => locationApi.companies(pincode, radiusKm),
    enabled: !!pincode && /^\d{6}$/.test(pincode),
  })
}

export function usePlacesSearch(pincode: string, radiusMeters: number, enabled: boolean) {
  return useQuery({
    queryKey: ['places', pincode, radiusMeters],
    queryFn: () => locationApi.placesSearch(pincode, radiusMeters),
    enabled: enabled && !!pincode && /^\d{6}$/.test(pincode),
    staleTime: 5 * 60 * 1000,
  })
}

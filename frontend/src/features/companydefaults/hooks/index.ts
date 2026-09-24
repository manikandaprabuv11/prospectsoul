import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { companyDefaultsApi } from '../api'
import type { CompanyDefaultFilterUpsertRequest } from '../types'

const KEY = ['company-default-filters'] as const

export function useCompanyDefaults() {
  return useQuery({ queryKey: KEY, queryFn: () => companyDefaultsApi.list(true) })
}

export function useCreateCompanyDefault() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: CompanyDefaultFilterUpsertRequest) => companyDefaultsApi.create(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  })
}

export function useUpdateCompanyDefault(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: CompanyDefaultFilterUpsertRequest) => companyDefaultsApi.update(id, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  })
}

export function useDeleteCompanyDefault() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => companyDefaultsApi.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  })
}

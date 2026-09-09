import { queryClient } from '@/lib/query-client'
import { useMutation, useQuery } from '@tanstack/react-query'
import { companyApi } from '../api'
import type { CompanyCreateRequest, CompanyFilters, CompanyUpdateRequest } from '../types'

export function useCompanies(filters: CompanyFilters) {
  return useQuery({
    queryKey: ['companies', filters],
    queryFn: () => companyApi.list(filters),
  })
}

export function useCompany(id: string | undefined) {
  return useQuery({
    queryKey: ['company', id],
    queryFn: () => companyApi.getById(id!),
    enabled: !!id,
  })
}

export function useCreateCompany() {
  return useMutation({
    mutationFn: (data: CompanyCreateRequest) => companyApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['companies'] })
    },
  })
}

export function useUpdateCompany(id: string) {
  return useMutation({
    mutationFn: (data: CompanyUpdateRequest) => companyApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['companies'] })
      queryClient.invalidateQueries({ queryKey: ['company', id] })
    },
  })
}

export function useVerifyCompany(id: string) {
  return useMutation({
    mutationFn: () => companyApi.verify(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['company', id] })
      queryClient.invalidateQueries({ queryKey: ['companies'] })
    },
  })
}

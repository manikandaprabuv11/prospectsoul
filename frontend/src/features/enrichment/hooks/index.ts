import { queryClient } from '@/lib/query-client'
import { useMutation, useQuery } from '@tanstack/react-query'
import { enrichmentApi } from '../api'
import type { EnrichCompanyRequest, ResolveCandidateRequest } from '../types'

export function useEnrichCompany(companyId: string) {
  return useMutation({
    mutationFn: (data: EnrichCompanyRequest) => enrichmentApi.enrichCompany(companyId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['enrichment-jobs', companyId] })
      queryClient.invalidateQueries({ queryKey: ['enrichment-candidates', companyId] })
      queryClient.invalidateQueries({ queryKey: ['company', companyId] })
    },
  })
}

export function useEnrichmentJobs(companyId: string | undefined) {
  return useQuery({
    queryKey: ['enrichment-jobs', companyId],
    queryFn: () => enrichmentApi.listJobsByCompany(companyId!, 0, 10),
    enabled: !!companyId,
  })
}

export function useEnrichmentCandidates(companyId: string | undefined, status?: string) {
  return useQuery({
    queryKey: ['enrichment-candidates', companyId, status],
    queryFn: () => enrichmentApi.listCandidatesByCompany(companyId!, status, 0, 50),
    enabled: !!companyId,
  })
}

export function useResolveCandidate(companyId: string) {
  return useMutation({
    mutationFn: ({ candidateId, data }: { candidateId: string; data: ResolveCandidateRequest }) =>
      enrichmentApi.resolveCandidate(candidateId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['enrichment-candidates', companyId] })
      queryClient.invalidateQueries({ queryKey: ['company', companyId] })
    },
  })
}

export function useProviderConfigs() {
  return useQuery({
    queryKey: ['provider-configs'],
    queryFn: () => enrichmentApi.listProviderConfigs(),
  })
}

export function useAllEnrichmentJobs(page = 0, size = 25) {
  return useQuery({
    queryKey: ['enrichment-jobs-all', page, size],
    queryFn: () => enrichmentApi.listAllJobs(page, size),
  })
}

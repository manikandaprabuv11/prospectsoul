import { apiClient } from '@/api/client'
import type {
  EnrichCompanyRequest,
  EnrichmentCandidate,
  EnrichmentJob,
  PageResponse,
  ProviderConfig,
  ResolveCandidateRequest,
} from '../types'

export const enrichmentApi = {
  enrichCompany(companyId: string, data: EnrichCompanyRequest) {
    return apiClient.post<EnrichmentJob[]>(`/api/v1/companies/${companyId}/enrich`, { body: data })
  },

  listJobsByCompany(companyId: string, page = 0, size = 25) {
    return apiClient.get<PageResponse<EnrichmentJob>>(`/api/v1/companies/${companyId}/enrichment-jobs`, {
      query: { page, size },
    })
  },

  listAllJobs(page = 0, size = 25) {
    return apiClient.get<PageResponse<EnrichmentJob>>('/api/v1/enrichment-jobs', {
      query: { page, size },
    })
  },

  getJob(jobId: string) {
    return apiClient.get<EnrichmentJob>(`/api/v1/enrichment-jobs/${jobId}`)
  },

  listCandidatesByCompany(companyId: string, status?: string, page = 0, size = 25) {
    return apiClient.get<PageResponse<EnrichmentCandidate>>(`/api/v1/companies/${companyId}/enrichment-candidates`, {
      query: { status, page, size },
    })
  },

  resolveCandidate(candidateId: string, data: ResolveCandidateRequest) {
    return apiClient.post<EnrichmentCandidate>(`/api/v1/enrichment-candidates/${candidateId}/resolve`, { body: data })
  },

  listProviderConfigs() {
    return apiClient.get<ProviderConfig[]>('/api/v1/admin/enrichment/providers')
  },

  updateProviderConfig(providerKey: string, data: Partial<ProviderConfig>) {
    return apiClient.patch<ProviderConfig>(`/api/v1/admin/enrichment/providers/${providerKey}`, { body: data })
  },
}

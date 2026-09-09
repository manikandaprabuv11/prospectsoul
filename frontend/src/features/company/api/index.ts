import { apiClient } from '@/api/client'
import type {
  Company,
  CompanyCreateRequest,
  CompanyFilters,
  CompanyUpdateRequest,
  PageResponse,
} from '../types'

export const companyApi = {
  list(filters: CompanyFilters = {}) {
    return apiClient.get<PageResponse<Company>>('/api/v1/companies', {
      query: {
        q: filters.q,
        city: filters.city,
        state: filters.state,
        industry: filters.industry,
        cluster: filters.cluster,
        source: filters.source,
        pipeline_state: filters.pipeline_state,
        verification_status: filters.verification_status,
        page: filters.page ?? 0,
        size: filters.size ?? 25,
        sort: filters.sort ?? 'createdAt',
        sort_dir: filters.sort_dir ?? 'desc',
      },
    })
  },

  getById(id: string) {
    return apiClient.get<Company>(`/api/v1/companies/${id}`)
  },

  create(data: CompanyCreateRequest) {
    return apiClient.post<Company>('/api/v1/companies', { body: data })
  },

  update(id: string, data: CompanyUpdateRequest) {
    return apiClient.patch<Company>(`/api/v1/companies/${id}`, { body: data })
  },

  verify(id: string) {
    return apiClient.post<Company>(`/api/v1/companies/${id}/verify`)
  },
}

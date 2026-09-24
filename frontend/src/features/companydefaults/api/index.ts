import { apiClient } from '@/api/client'
import type { CompanyDefaultFilter, CompanyDefaultFilterUpsertRequest } from '../types'

const BASE = '/company-default-filters'

export const companyDefaultsApi = {
  list(includeInactive = true): Promise<CompanyDefaultFilter[]> {
    return apiClient.get<CompanyDefaultFilter[]>(BASE, {
      query: { include_inactive: includeInactive },
    })
  },
  create(body: CompanyDefaultFilterUpsertRequest): Promise<CompanyDefaultFilter> {
    return apiClient.post<CompanyDefaultFilter>(BASE, { body })
  },
  update(id: string, body: CompanyDefaultFilterUpsertRequest): Promise<CompanyDefaultFilter> {
    return apiClient.patch<CompanyDefaultFilter>(`${BASE}/${id}`, { body })
  },
  delete(id: string): Promise<void> {
    return apiClient.delete(`${BASE}/${id}`)
  },
}

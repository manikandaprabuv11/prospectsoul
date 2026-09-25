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
        region: filters.region,
        district: filters.district,
        pincode: filters.pincode,
        turnover_min: filters.turnover_min,
        turnover_max: filters.turnover_max,
        employee_min: filters.employee_min,
        employee_max: filters.employee_max,
        gst_present: filters.gst_present,
        nic_code_id: filters.nic_code_id,
        nic_parent_ids: filters.nic_parent_ids,
        // grouped_by_nic is inherently single-parent on the backend — send
        // the singular alias too so that view keeps working when exactly
        // one NIC is selected (the UI only offers that view in that case).
        nic_parent_id: filters.view === 'grouped_by_nic' ? filters.nic_parent_ids?.[0] : undefined,
        nic_include_descendants: filters.nic_include_descendants,
        has_contact_role_id: filters.has_contact_role_id,
        view: filters.view,
        apply_defaults: filters.apply_defaults ?? true,
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

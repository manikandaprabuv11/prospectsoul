import { apiClient } from '@/api/client'
import type { PageResponse } from '@/features/company/types'
import type {
  NicCodeCreateRequest,
  NicCodeResponse,
  NicCodeUpdateRequest,
  NicImportResultResponse,
  NicListFilters,
  NicTreeNodeResponse,
} from '../types'

const BASE = '/nic-codes'

export const nicApi = {
  list(filters: NicListFilters = {}): Promise<PageResponse<NicCodeResponse>> {
    return apiClient.get<PageResponse<NicCodeResponse>>(BASE, {
      query: {
        q: filters.q,
        level: filters.level,
        industry_type: filters.industry_type,
        is_primary: filters.is_primary,
        active: filters.active,
        page: filters.page ?? 0,
        size: filters.size ?? 50,
        sort: filters.sort ?? 'code',
        sort_dir: filters.sort_dir ?? 'asc',
      },
    })
  },

  getById(id: string): Promise<NicCodeResponse> {
    return apiClient.get<NicCodeResponse>(`${BASE}/${id}`)
  },

  children(id: string): Promise<NicCodeResponse[]> {
    return apiClient.get<NicCodeResponse[]>(`${BASE}/${id}/children`)
  },

  tree(rootId?: string, depth = 3): Promise<NicTreeNodeResponse[]> {
    return apiClient.get<NicTreeNodeResponse[]>(`${BASE}/tree`, {
      query: { root_id: rootId, depth },
    })
  },

  primary(): Promise<NicCodeResponse[]> {
    return apiClient.get<NicCodeResponse[]>(`${BASE}/primary`)
  },

  create(body: NicCodeCreateRequest): Promise<NicCodeResponse> {
    return apiClient.post<NicCodeResponse>(BASE, { body })
  },

  update(id: string, body: NicCodeUpdateRequest): Promise<NicCodeResponse> {
    return apiClient.patch<NicCodeResponse>(`${BASE}/${id}`, { body })
  },

  togglePrimary(id: string): Promise<NicCodeResponse> {
    return apiClient.post<NicCodeResponse>(`${BASE}/${id}/toggle-primary`)
  },

  importFile(file: File): Promise<NicImportResultResponse> {
    const fd = new FormData()
    fd.append('file', file)
    return apiClient.post<NicImportResultResponse>('/admin/nic-codes/import', { formData: fd })
  },
}

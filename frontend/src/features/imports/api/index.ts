import { apiClient } from '@/api/client'
import type {
  ColumnMapping,
  ImportBatch,
  ImportPreview,
  ImportRow,
  MappingSuggestionResponse,
  PageResponse,
} from '../types'

export const importApi = {
  upload(file: File, source: string = 'EXCEL_CSV') {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('source', source)
    return apiClient.post<ImportBatch>('/api/v1/imports', { formData })
  },

  list(page = 0, size = 25) {
    return apiClient.get<PageResponse<ImportBatch>>('/api/v1/imports', {
      query: { page, size },
    })
  },

  getById(id: string) {
    return apiClient.get<ImportBatch>(`/api/v1/imports/${id}`)
  },

  getRows(id: string, page = 0, size = 25) {
    return apiClient.get<PageResponse<ImportRow>>(`/api/v1/imports/${id}/rows`, {
      query: { page, size },
    })
  },

  suggestMappings(id: string) {
    return apiClient.get<MappingSuggestionResponse>(`/api/v1/imports/${id}/mappings/suggest`)
  },

  confirmMappings(id: string, mappings: ColumnMapping[]) {
    return apiClient.post<ImportBatch>(`/api/v1/imports/${id}/mappings/confirm`, {
      body: { mappings },
    })
  },

  preview(id: string) {
    return apiClient.get<ImportPreview>(`/api/v1/imports/${id}/preview`)
  },

  startProcessing(id: string) {
    return apiClient.post<ImportBatch>(`/api/v1/imports/${id}/process`)
  },
}

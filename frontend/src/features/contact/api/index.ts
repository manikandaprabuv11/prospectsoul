import { apiClient } from '@/api/client'
import type { ContactCreateRequest, ContactResponse, ContactUpdateRequest } from '../types'

export const contactsApi = {
  listForCompany(companyId: string): Promise<ContactResponse[]> {
    return apiClient.get<ContactResponse[]>(`/companies/${companyId}/contacts`)
  },
  create(companyId: string, body: ContactCreateRequest): Promise<ContactResponse> {
    return apiClient.post<ContactResponse>(`/companies/${companyId}/contacts`, { body })
  },
  update(contactId: string, body: ContactUpdateRequest): Promise<ContactResponse> {
    return apiClient.patch<ContactResponse>(`/contacts/${contactId}`, { body })
  },
  makePrimary(contactId: string): Promise<ContactResponse> {
    return apiClient.post<ContactResponse>(`/contacts/${contactId}/make-primary`)
  },
}

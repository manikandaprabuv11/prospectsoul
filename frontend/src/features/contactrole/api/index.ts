import { apiClient } from '@/api/client'
import type {
  ContactRoleCreateRequest,
  ContactRoleResponse,
  ContactRoleUpdateRequest,
} from '../types'

const BASE = '/contact-roles'

export const contactRolesApi = {
  list(includeInactive = true): Promise<ContactRoleResponse[]> {
    return apiClient.get<ContactRoleResponse[]>(BASE, {
      query: { include_inactive: includeInactive },
    })
  },
  create(body: ContactRoleCreateRequest): Promise<ContactRoleResponse> {
    return apiClient.post<ContactRoleResponse>(BASE, { body })
  },
  update(id: string, body: ContactRoleUpdateRequest): Promise<ContactRoleResponse> {
    return apiClient.patch<ContactRoleResponse>(`${BASE}/${id}`, { body })
  },
}

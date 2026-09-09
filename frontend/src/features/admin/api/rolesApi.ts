import { apiClient } from '@/api/client'
import type { UserResponse } from '../types/user'
import type { RoleDetailResponse, RoleResponse, RoleUpdateRequest } from '../types/role'

const BASE = '/admin/roles'

export const rolesApi = {
  list(): Promise<RoleResponse[]> {
    return apiClient.get<RoleResponse[]>(BASE)
  },

  getById(id: string): Promise<RoleDetailResponse> {
    return apiClient.get<RoleDetailResponse>(`${BASE}/${id}`)
  },

  update(id: string, data: RoleUpdateRequest): Promise<RoleResponse> {
    return apiClient.patch<RoleResponse>(`${BASE}/${id}`, { body: data })
  },

  getAssignedUsers(id: string): Promise<UserResponse[]> {
    return apiClient.get<UserResponse[]>(`${BASE}/${id}/users`)
  },

  assignUser(roleId: string, userId: string): Promise<void> {
    return apiClient.post(`${BASE}/${roleId}/users`, { body: { user_id: userId } })
  },

  unassignUser(roleId: string, userId: string): Promise<void> {
    return apiClient.delete(`${BASE}/${roleId}/users/${userId}`)
  },
}

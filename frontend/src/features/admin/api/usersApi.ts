import { apiClient } from '@/api/client'
import type { UserCreateRequest, UserResponse, UserUpdateRequest, UsersPageResponse } from '../types/user'

const BASE = '/admin/users'

export interface UserListParams {
  q?: string
  role?: string
  active?: boolean
  page?: number
  size?: number
  sort?: string
  sort_dir?: string
}

export const usersApi = {
  list(params: UserListParams = {}): Promise<UsersPageResponse> {
    return apiClient.get<UsersPageResponse>(BASE, {
      query: {
        q: params.q || undefined,
        role: params.role || undefined,
        active: params.active !== undefined ? params.active : undefined,
        page: params.page ?? 0,
        size: params.size ?? 25,
        sort: params.sort ?? 'fullName',
        sort_dir: params.sort_dir ?? 'asc',
      },
    })
  },

  getById(id: string): Promise<UserResponse> {
    return apiClient.get<UserResponse>(`${BASE}/${id}`)
  },

  create(data: UserCreateRequest): Promise<UserResponse> {
    return apiClient.post<UserResponse>(BASE, { body: data })
  },

  update(id: string, data: UserUpdateRequest): Promise<UserResponse> {
    return apiClient.patch<UserResponse>(`${BASE}/${id}`, { body: data })
  },

  activate(id: string): Promise<UserResponse> {
    return apiClient.post<UserResponse>(`${BASE}/${id}/activate`)
  },

  deactivate(id: string): Promise<UserResponse> {
    return apiClient.post<UserResponse>(`${BASE}/${id}/deactivate`)
  },
}

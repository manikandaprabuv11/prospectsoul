import type { UserResponse } from './user'

export interface RoleResponse {
  id: string
  name: string
  display_name: string
  description: string
  permissions: string[]
  user_count: number
  active: boolean
}

export interface RoleDetailResponse extends RoleResponse {
  assigned_users: UserResponse[]
}

export interface RoleUpdateRequest {
  display_name?: string
  description?: string
}

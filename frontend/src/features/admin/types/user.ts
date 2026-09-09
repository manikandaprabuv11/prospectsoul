export interface UserResponse {
  id: string
  username: string
  full_name: string
  email: string
  role: string
  role_display_name: string
  active: boolean
  last_login_at: string | null
  created_at: string
}

export interface UserCreateRequest {
  username: string
  full_name: string
  email: string
  password: string
  role: string
}

export interface UserUpdateRequest {
  full_name?: string
  email?: string
  role?: string
}

export interface UsersPageResponse {
  content: UserResponse[]
  page: number
  size: number
  total_elements: number
  total_pages: number
}

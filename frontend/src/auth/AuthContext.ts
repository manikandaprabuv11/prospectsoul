import { createContext } from 'react'

export interface AuthUser {
  id: string
  username: string
  fullName: string
  email: string
}

export interface AuthContextValue {
  authenticated: boolean
  user: AuthUser | null
  roles: string[]
  token: string | undefined
  hasRole: (role: string) => boolean
  logout: () => void
}

export interface LoginFn {
  (username: string, password: string): Promise<{ success: boolean; error?: string }>
}

export const AuthContext = createContext<AuthContextValue | null>(null)

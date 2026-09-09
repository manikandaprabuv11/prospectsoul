import type { ReactNode } from 'react'
import { Navigate } from 'react-router'
import { useAuth } from './useAuth'

interface RequireRoleProps {
  roles: string[]
  children: ReactNode
}

export function RequireRole({ roles, children }: RequireRoleProps) {
  const { hasRole } = useAuth()

  if (!roles.some(hasRole)) {
    return <Navigate to="/" replace />
  }

  return children
}

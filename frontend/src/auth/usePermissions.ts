import { useMemo } from 'react'
import { useAuth } from './useAuth'

export interface Permissions {
  canRead: boolean
  canMutate: boolean
  canExport: boolean
  canConfigure: boolean
}

const MUTATE_ROLES = ['PS_ANALYST', 'PS_SALES_LEAD', 'PS_ADMIN']
const EXPORT_ROLES = ['PS_SALES_LEAD', 'PS_ADMIN']
const CONFIGURE_ROLES = ['PS_ADMIN']
const READ_ROLES = ['PS_ANALYST', 'PS_SALES_LEAD', 'PS_ADMIN', 'PS_VIEWER', 'PS_COO']

export function usePermissions(): Permissions {
  const { roles } = useAuth()

  return useMemo(() => ({
    canRead: roles.some((r) => READ_ROLES.includes(r)),
    canMutate: roles.some((r) => MUTATE_ROLES.includes(r)),
    canExport: roles.some((r) => EXPORT_ROLES.includes(r)),
    canConfigure: roles.some((r) => CONFIGURE_ROLES.includes(r)),
  }), [roles])
}

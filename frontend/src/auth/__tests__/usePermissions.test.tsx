import { renderHook } from '@testing-library/react'
import type { ReactNode } from 'react'
import { AuthContext } from '../AuthContext'
import type { AuthContextValue } from '../AuthContext'
import { usePermissions } from '../usePermissions'
import { describe, it, expect, vi } from 'vitest'

function wrapper(roles: string[]) {
  const authValue: AuthContextValue = {
    authenticated: true,
    user: { id: '1', username: 'test', fullName: 'Test', email: 'test@test.com' },
    roles,
    token: 'token',
    hasRole: (r) => roles.includes(r),
    logout: vi.fn(),
  }

  return ({ children }: { children: ReactNode }) => (
    <AuthContext value={authValue}>{children}</AuthContext>
  )
}

describe('usePermissions', () => {
  it('analyst can read and mutate but not export or configure', () => {
    const { result } = renderHook(() => usePermissions(), { wrapper: wrapper(['PS_ANALYST']) })
    expect(result.current.canRead).toBe(true)
    expect(result.current.canMutate).toBe(true)
    expect(result.current.canExport).toBe(false)
    expect(result.current.canConfigure).toBe(false)
  })

  it('sales lead can read, mutate, and export but not configure', () => {
    const { result } = renderHook(() => usePermissions(), { wrapper: wrapper(['PS_SALES_LEAD']) })
    expect(result.current.canRead).toBe(true)
    expect(result.current.canMutate).toBe(true)
    expect(result.current.canExport).toBe(true)
    expect(result.current.canConfigure).toBe(false)
  })

  it('admin has all permissions', () => {
    const { result } = renderHook(() => usePermissions(), { wrapper: wrapper(['PS_ADMIN']) })
    expect(result.current.canRead).toBe(true)
    expect(result.current.canMutate).toBe(true)
    expect(result.current.canExport).toBe(true)
    expect(result.current.canConfigure).toBe(true)
  })

  it('viewer can only read', () => {
    const { result } = renderHook(() => usePermissions(), { wrapper: wrapper(['PS_VIEWER']) })
    expect(result.current.canRead).toBe(true)
    expect(result.current.canMutate).toBe(false)
    expect(result.current.canExport).toBe(false)
    expect(result.current.canConfigure).toBe(false)
  })

  it('COO can only read', () => {
    const { result } = renderHook(() => usePermissions(), { wrapper: wrapper(['PS_COO']) })
    expect(result.current.canRead).toBe(true)
    expect(result.current.canMutate).toBe(false)
    expect(result.current.canExport).toBe(false)
    expect(result.current.canConfigure).toBe(false)
  })
})

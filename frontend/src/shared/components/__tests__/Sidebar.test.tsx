import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { AuthContext } from '@/auth/AuthContext'
import type { AuthContextValue } from '@/auth/AuthContext'
import { Sidebar } from '../Sidebar'
import { describe, it, expect, vi } from 'vitest'

function renderWithRole(roles: string[]) {
  const authValue: AuthContextValue = {
    authenticated: true,
    user: { id: '1', username: 'test', fullName: 'Test User', email: 'test@test.com' },
    roles,
    token: 'token',
    hasRole: (r) => roles.includes(r),
    logout: vi.fn(),
  }

  return render(
    <AuthContext value={authValue}>
      <MemoryRouter>
        <Sidebar />
      </MemoryRouter>
    </AuthContext>,
  )
}

describe('Sidebar', () => {
  it('shows all nav items for admin', () => {
    renderWithRole(['PS_ADMIN'])
    expect(screen.getByText('Dashboard')).toBeInTheDocument()
    expect(screen.getByText('Companies')).toBeInTheDocument()
    expect(screen.getByText('Imports')).toBeInTheDocument()
    expect(screen.getByText('Users & Roles')).toBeInTheDocument()
  })

  it('hides admin items for viewer', () => {
    renderWithRole(['PS_VIEWER'])
    expect(screen.getByText('Dashboard')).toBeInTheDocument()
    expect(screen.getByText('Companies')).toBeInTheDocument()
    expect(screen.queryByText('Imports')).not.toBeInTheDocument()
    expect(screen.queryByText('Users & Roles')).not.toBeInTheDocument()
  })

  it('hides admin items for COO', () => {
    renderWithRole(['PS_COO'])
    expect(screen.getByText('Dashboard')).toBeInTheDocument()
    expect(screen.getByText('Companies')).toBeInTheDocument()
    expect(screen.queryByText('Imports')).not.toBeInTheDocument()
    expect(screen.queryByText('Users & Roles')).not.toBeInTheDocument()
  })

  it('shows imports but hides admin for analyst', () => {
    renderWithRole(['PS_ANALYST'])
    expect(screen.getByText('Dashboard')).toBeInTheDocument()
    expect(screen.getByText('Companies')).toBeInTheDocument()
    expect(screen.getByText('Imports')).toBeInTheDocument()
    expect(screen.queryByText('Users & Roles')).not.toBeInTheDocument()
  })

  it('shows imports but hides admin for sales lead', () => {
    renderWithRole(['PS_SALES_LEAD'])
    expect(screen.getByText('Dashboard')).toBeInTheDocument()
    expect(screen.getByText('Companies')).toBeInTheDocument()
    expect(screen.getByText('Imports')).toBeInTheDocument()
    expect(screen.queryByText('Users & Roles')).not.toBeInTheDocument()
  })
})

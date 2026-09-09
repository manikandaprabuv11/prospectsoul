import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { AuthContext } from '../AuthContext'
import type { AuthContextValue } from '../AuthContext'
import { RequireRole } from '../RequireRole'
import { describe, it, expect, vi } from 'vitest'

function renderWithAuth(roles: string[], requiredRoles: string[]) {
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
      <MemoryRouter initialEntries={['/protected']}>
        <Routes>
          <Route path="/" element={<div>Home (redirected)</div>} />
          <Route
            path="/protected"
            element={
              <RequireRole roles={requiredRoles}>
                <div>Protected Content</div>
              </RequireRole>
            }
          />
        </Routes>
      </MemoryRouter>
    </AuthContext>,
  )
}

describe('RequireRole', () => {
  it('renders children when user has required role', () => {
    renderWithAuth(['PS_ADMIN'], ['PS_ADMIN'])
    expect(screen.getByText('Protected Content')).toBeInTheDocument()
  })

  it('renders children when user has one of multiple required roles', () => {
    renderWithAuth(['PS_ANALYST'], ['PS_ANALYST', 'PS_SALES_LEAD', 'PS_ADMIN'])
    expect(screen.getByText('Protected Content')).toBeInTheDocument()
  })

  it('redirects to home when user lacks required role', () => {
    renderWithAuth(['PS_VIEWER'], ['PS_ADMIN'])
    expect(screen.getByText('Home (redirected)')).toBeInTheDocument()
    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument()
  })

  it('redirects viewer from admin routes', () => {
    renderWithAuth(['PS_VIEWER'], ['PS_ADMIN'])
    expect(screen.getByText('Home (redirected)')).toBeInTheDocument()
  })

  it('redirects COO from mutate routes', () => {
    renderWithAuth(['PS_COO'], ['PS_ANALYST', 'PS_SALES_LEAD', 'PS_ADMIN'])
    expect(screen.getByText('Home (redirected)')).toBeInTheDocument()
  })
})

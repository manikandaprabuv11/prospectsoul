import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AuthContext } from '@/auth/AuthContext'
import type { AuthContextValue } from '@/auth/AuthContext'
import { UserMenu } from '../UserMenu'
import { describe, it, expect, vi } from 'vitest'

function renderWithAuth(overrides: Partial<AuthContextValue> = {}) {
  const authValue: AuthContextValue = {
    authenticated: true,
    user: { id: '1', username: 'admin', fullName: 'Ravi Chandran', email: 'ravi@vyoog.com' },
    roles: ['PS_ADMIN'],
    token: 'token',
    hasRole: (r) => ['PS_ADMIN'].includes(r),
    logout: vi.fn(),
    ...overrides,
  }

  return {
    ...render(
      <AuthContext value={authValue}>
        <UserMenu />
      </AuthContext>,
    ),
    authValue,
  }
}

describe('UserMenu', () => {
  it('renders user initials and name', () => {
    renderWithAuth()
    expect(screen.getByText('RC')).toBeInTheDocument()
    expect(screen.getByText('Ravi Chandran')).toBeInTheDocument()
    expect(screen.getByText('Admin')).toBeInTheDocument()
  })

  it('opens dropdown on click', async () => {
    const user = userEvent.setup()
    renderWithAuth()

    await user.click(screen.getByText('RC'))
    expect(screen.getByText('ravi@vyoog.com')).toBeInTheDocument()
    expect(screen.getByText('Sign out')).toBeInTheDocument()
  })

  it('calls logout on sign out click', async () => {
    const user = userEvent.setup()
    const logout = vi.fn()
    renderWithAuth({ logout })

    await user.click(screen.getByText('RC'))
    await user.click(screen.getByText('Sign out'))
    expect(logout).toHaveBeenCalled()
  })

  it('shows correct role label for analyst', () => {
    renderWithAuth({
      user: { id: '2', username: 'analyst', fullName: 'Priya Sharma', email: 'priya@vyoog.com' },
      roles: ['PS_ANALYST'],
    })
    expect(screen.getByText('PS')).toBeInTheDocument()
    expect(screen.getByText('Analyst')).toBeInTheDocument()
  })

  it('returns null when user is null', () => {
    const { container } = render(
      <AuthContext value={{
        authenticated: false,
        user: null,
        roles: [],
        token: undefined,
        hasRole: () => false,
        logout: vi.fn(),
      }}>
        <UserMenu />
      </AuthContext>,
    )
    expect(container.firstChild).toBeNull()
  })
})

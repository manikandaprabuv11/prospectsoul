import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { LoginPage } from '../LoginPage'
import { describe, it, expect, vi } from 'vitest'

describe('LoginPage', () => {
  const defaultProps = {
    onLogin: vi.fn().mockResolvedValue({ success: true }),
    onForgotPassword: vi.fn(),
  }

  it('renders sign-in form with username and password fields', () => {
    render(<LoginPage {...defaultProps} />)
    expect(screen.getByLabelText(/username or email/i)).toBeInTheDocument()
    expect(screen.getByLabelText('Password')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument()
  })

  it('disables submit when fields are empty', () => {
    render(<LoginPage {...defaultProps} />)
    expect(screen.getByRole('button', { name: /sign in/i })).toBeDisabled()
  })

  it('calls onLogin with credentials on submit', async () => {
    const onLogin = vi.fn().mockResolvedValue({ success: true })
    const user = userEvent.setup()
    render(<LoginPage {...defaultProps} onLogin={onLogin} />)

    await user.type(screen.getByLabelText(/username or email/i), 'analyst')
    await user.type(screen.getByLabelText('Password'), 'password123')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    expect(onLogin).toHaveBeenCalledWith('analyst', 'password123')
  })

  it('shows error message on failed login', async () => {
    const onLogin = vi.fn().mockResolvedValue({ success: false, error: 'Invalid username or password' })
    const user = userEvent.setup()
    render(<LoginPage {...defaultProps} onLogin={onLogin} />)

    await user.type(screen.getByLabelText(/username or email/i), 'bad')
    await user.type(screen.getByLabelText('Password'), 'wrong')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    expect(await screen.findByText('Invalid username or password')).toBeInTheDocument()
  })

  it('calls onForgotPassword when forgot link clicked', async () => {
    const onForgotPassword = vi.fn()
    const user = userEvent.setup()
    render(<LoginPage {...defaultProps} onForgotPassword={onForgotPassword} />)

    await user.click(screen.getByRole('button', { name: /forgot password/i }))
    expect(onForgotPassword).toHaveBeenCalled()
  })

  it('toggles password visibility', async () => {
    const user = userEvent.setup()
    render(<LoginPage {...defaultProps} />)

    const passwordInput = screen.getByLabelText('Password')
    expect(passwordInput).toHaveAttribute('type', 'password')

    await user.click(screen.getByRole('button', { name: /show password/i }))
    expect(passwordInput).toHaveAttribute('type', 'text')
  })
})

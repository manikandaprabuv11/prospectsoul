import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ForgotPasswordPage } from '../ForgotPasswordPage'
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/api/client', () => ({
  apiClient: {
    post: vi.fn().mockResolvedValue({}),
  },
}))

describe('ForgotPasswordPage', () => {
  const onBack = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders email form initially', () => {
    render(<ForgotPasswordPage onBack={onBack} />)
    expect(screen.getByRole('heading', { name: /reset your password/i })).toBeInTheDocument()
    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /send reset link/i })).toBeInTheDocument()
  })

  it('calls onBack when back link is clicked', async () => {
    const user = userEvent.setup()
    render(<ForgotPasswordPage onBack={onBack} />)

    await user.click(screen.getByText(/back to sign in/i))
    expect(onBack).toHaveBeenCalled()
  })

  it('shows success state after submitting email', async () => {
    const user = userEvent.setup()
    render(<ForgotPasswordPage onBack={onBack} />)

    await user.type(screen.getByLabelText(/email address/i), 'priya@vyoog.com')
    await user.click(screen.getByRole('button', { name: /send reset link/i }))

    expect(await screen.findByRole('heading', { name: /check your email/i })).toBeInTheDocument()
    expect(screen.getByText('priya@vyoog.com')).toBeInTheDocument()
  })

  it('allows trying a different email after success', async () => {
    const user = userEvent.setup()
    render(<ForgotPasswordPage onBack={onBack} />)

    await user.type(screen.getByLabelText(/email address/i), 'test@vyoog.com')
    await user.click(screen.getByRole('button', { name: /send reset link/i }))

    await screen.findByRole('heading', { name: /check your email/i })
    await user.click(screen.getByText(/try a different email/i))

    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument()
  })
})

import { useState } from 'react'
import type { FormEvent } from 'react'
import { apiClient } from '@/api/client'
import './LoginPage.css'
import './ForgotPasswordPage.css'

const LogoSvg = () => (
  <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" stroke="#fff" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
)

const BackArrowSvg = () => (
  <svg viewBox="0 0 16 16" fill="none">
    <path d="M10 12L6 8l4-4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
)

const LockSvg = () => (
  <svg viewBox="0 0 24 24" fill="none">
    <rect x="3" y="11" width="18" height="11" rx="2" stroke="currentColor" strokeWidth="2"/>
    <path d="M7 11V7a5 5 0 0110 0v4" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
    <circle cx="12" cy="16" r="1.5" fill="currentColor"/>
  </svg>
)

const CheckSvg = () => (
  <svg viewBox="0 0 24 24" fill="none">
    <path d="M20 6L9 17l-5-5" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
)

export function ForgotPasswordPage({ onBack }: { onBack: () => void }) {
  const [email, setEmail] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [submitted, setSubmitted] = useState(false)
  const [submittedEmail, setSubmittedEmail] = useState('')

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (!email.trim()) return

    setSubmitting(true)
    try {
      await apiClient.post('/auth/forgot-password', { body: { email: email.trim() } })
    } catch {
      // Always show success — no email enumeration
    }
    setSubmittedEmail(email.trim())
    setSubmitted(true)
    setSubmitting(false)
  }

  const handleTryAgain = () => {
    setSubmitted(false)
    setEmail('')
    setSubmittedEmail('')
  }

  return (
    <div className="login-shell">
      {/* LEFT: Brand panel (identical to login) */}
      <div className="brand-panel">
        <div className="logo-row">
          <div className="logo-mark">
            <LogoSvg />
          </div>
          <span className="logo-text">ProspectSoul</span>
        </div>

        <div className="brand-hero">
          <h1>Never lose track of a prospect again</h1>
          <p>Every company, every research finding, every qualification decision — tracked permanently with evidence you can verify in seconds.</p>
        </div>

        <div style={{ height: 1 }} />
      </div>

      {/* RIGHT: Form panel */}
      <div className="form-panel">
        <div className="form-container">
          <div className="mobile-brand">
            <div className="logo-mark">
              <LogoSvg />
            </div>
            <span className="logo-text-dark">ProspectSoul</span>
          </div>

          {!submitted ? (
            <div className="request-state">
              <button type="button" className="back-link" onClick={onBack}>
                <BackArrowSvg />
                Back to sign in
              </button>

              <div className="form-icon">
                <LockSvg />
              </div>

              <div className="form-header">
                <h2>Reset your password</h2>
                <p>Enter the email address linked to your account and we'll send you a link to reset your password.</p>
              </div>

              <form onSubmit={handleSubmit}>
                <div className="field-group">
                  <label htmlFor="forgot-email">Email address</label>
                  <input
                    type="email"
                    id="forgot-email"
                    placeholder="priya@vyoog.com"
                    autoComplete="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    disabled={submitting}
                  />
                </div>
                <button
                  type="submit"
                  className="btn-login"
                  disabled={submitting || !email.trim()}
                >
                  {submitting ? (
                    <>
                      <span className="spinner" />
                      Sending…
                    </>
                  ) : (
                    'Send reset link'
                  )}
                </button>
              </form>

              <div className="form-footer">
                Vyoog Information Private Limited — internal use only
              </div>
            </div>
          ) : (
            <div className="success-state">
              <button type="button" className="back-link" onClick={onBack}>
                <BackArrowSvg />
                Back to sign in
              </button>

              <div className="success-icon">
                <CheckSvg />
              </div>

              <h2>Check your email</h2>
              <p>We've sent a password reset link to <span className="email-highlight">{submittedEmail}</span></p>
              <p className="help-text">Didn't receive it? Check your spam folder or try again in a few minutes.</p>

              <button type="button" className="btn-outline" onClick={handleTryAgain}>
                Try a different email
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

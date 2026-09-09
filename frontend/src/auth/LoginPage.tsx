import { useState } from 'react'
import type { FormEvent } from 'react'
import type { LoginFn } from './AuthContext'
import './LoginPage.css'

const LogoSvg = () => (
  <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" stroke="#fff" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
)

const EyeSvg = () => (
  <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
    <path d="M1.5 10s3-6 8.5-6 8.5 6 8.5 6-3 6-8.5 6-8.5-6-8.5-6z" stroke="currentColor" strokeWidth="1.5"/>
    <circle cx="10" cy="10" r="2.5" stroke="currentColor" strokeWidth="1.5"/>
  </svg>
)

const EyeOffSvg = () => (
  <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
    <path d="M1.5 10s3-6 8.5-6 8.5 6 8.5 6-3 6-8.5 6-8.5-6-8.5-6z" stroke="currentColor" strokeWidth="1.5"/>
    <circle cx="10" cy="10" r="2.5" stroke="currentColor" strokeWidth="1.5"/>
    <path d="M3 3l14 14" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
  </svg>
)

const ErrorIcon = () => (
  <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
    <circle cx="9" cy="9" r="8" stroke="currentColor" strokeWidth="1.5"/>
    <path d="M9 5.5v4M9 12h.007" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
  </svg>
)

export function LoginPage({ onLogin, onForgotPassword }: { onLogin: LoginFn; onForgotPassword: () => void }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [rememberMe, setRememberMe] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (!username.trim() || !password) return

    setError(null)
    setSubmitting(true)

    const result = await onLogin(username.trim(), password)
    if (!result.success) {
      setError(result.error ?? 'Authentication failed')
      setSubmitting(false)
    }
  }

  return (
    <div className="login-shell">
      {/* LEFT: Brand panel */}
      <div className="brand-panel">
        <div className="logo-row">
          <div className="logo-mark">
            <LogoSvg />
          </div>
          <span className="logo-text">ProspectSoul</span>
        </div>

        <div className="brand-hero">
          <h1>Your company intelligence starts here</h1>
          <p>One record per company. Every research decision tracked. Every qualification backed by evidence. No prospect ever re-researched from zero.</p>
        </div>

        <div className="brand-stats">
          <div className="stat-item">
            <div className="stat-num">100k</div>
            <div className="stat-label">companies tracked</div>
          </div>
          <div className="stat-item">
            <div className="stat-num">6</div>
            <div className="stat-label">pipeline stages</div>
          </div>
          <div className="stat-item">
            <div className="stat-num">10s</div>
            <div className="stat-label">to answer &quot;seen before?&quot;</div>
          </div>
        </div>
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

          <div className="form-header">
            <h2>Sign in</h2>
            <p>Enter your credentials to access the platform</p>
          </div>

          {error && (
            <div className="login-error">
              <ErrorIcon />
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <div className="field-group">
              <label htmlFor="username">Username or email</label>
              <input
                type="text"
                id="username"
                placeholder="priya@vyoog.com"
                autoComplete="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                disabled={submitting}
              />
            </div>

            <div className="field-group">
              <label htmlFor="password">Password</label>
              <div className="password-wrap">
                <input
                  type={showPassword ? 'text' : 'password'}
                  id="password"
                  placeholder="Enter your password"
                  autoComplete="current-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  disabled={submitting}
                />
                <button
                  type="button"
                  className="password-toggle"
                  tabIndex={-1}
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                  onClick={() => setShowPassword(!showPassword)}
                >
                  {showPassword ? <EyeOffSvg /> : <EyeSvg />}
                </button>
              </div>
            </div>

            <div className="options-row">
              <label className="remember-me">
                <input
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(e) => setRememberMe(e.target.checked)}
                />
                Remember me
              </label>
              <button type="button" className="forgot-link" onClick={onForgotPassword}>Forgot password?</button>
            </div>

            <button
              type="submit"
              className="btn-login"
              disabled={submitting || !username.trim() || !password}
            >
              {submitting ? (
                <>
                  <span className="spinner" />
                  Signing in…
                </>
              ) : (
                'Sign in'
              )}
            </button>
          </form>

          <div className="form-footer">
            Vyoog Information Private Limited — internal use only
          </div>
        </div>
      </div>
    </div>
  )
}

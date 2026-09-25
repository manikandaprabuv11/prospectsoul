import { useState } from 'react'
import type { FormEvent } from 'react'
import { Eye, EyeOff, AlertCircle, Layers, Loader2 } from 'lucide-react'
import type { LoginFn } from './AuthContext'

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
    <div className="grid min-h-screen grid-cols-1 bg-surface-0 lg:grid-cols-2">
      {/* LEFT: Brand panel */}
      <div className="relative hidden flex-col justify-between overflow-hidden bg-brand-gradient p-12 text-white lg:flex">
        <div className="absolute -top-24 -right-24 size-80 rounded-full bg-white/8 blur-3xl" aria-hidden="true" />
        <div className="absolute -bottom-32 -left-24 size-72 rounded-full bg-white/6 blur-3xl" aria-hidden="true" />
        <div className="absolute top-1/2 left-1/3 size-56 rounded-full bg-white/5 blur-3xl" aria-hidden="true" />

        <div className="relative flex items-center gap-3.5">
          <div className="flex size-11 items-center justify-center rounded-xl bg-white/15 backdrop-blur-sm">
            <Layers className="size-5.5" />
          </div>
          <span className="text-[22px] font-bold tracking-tight">ProspectSoul</span>
        </div>

        <div className="relative max-w-sm space-y-5">
          <h1 className="text-4xl font-bold leading-[1.15] tracking-tight">
            Your company intelligence starts here
          </h1>
          <p className="text-[15px] leading-relaxed text-white/70">
            One record per company. Every research decision tracked. Every qualification backed by evidence. No prospect ever re-researched from zero.
          </p>
        </div>

        <div className="relative flex gap-10">
          <div>
            <div className="text-3xl font-bold text-white/90">100k</div>
            <div className="mt-0.5 text-[13px] text-white/50">companies tracked</div>
          </div>
          <div>
            <div className="text-3xl font-bold text-white/90">6</div>
            <div className="mt-0.5 text-[13px] text-white/50">pipeline stages</div>
          </div>
          <div>
            <div className="text-3xl font-bold text-white/90">10s</div>
            <div className="mt-0.5 text-[13px] text-white/50">to answer &quot;seen before?&quot;</div>
          </div>
        </div>
      </div>

      {/* RIGHT: Form panel */}
      <div className="flex items-center justify-center px-6 py-12 sm:px-12">
        <div className="w-full max-w-[400px]">
          {/* Mobile brand */}
          <div className="mb-8 flex items-center gap-2.5 lg:hidden">
            <div className="flex size-9 items-center justify-center rounded-lg bg-brand-gradient text-white">
              <Layers className="size-4.5" />
            </div>
            <span className="text-lg font-bold tracking-tight">ProspectSoul</span>
          </div>

          <div className="mb-10">
            <h2 className="text-[28px] font-bold tracking-tight">Sign in</h2>
            <p className="mt-2 text-[15px] leading-relaxed text-muted-foreground">
              Enter your credentials to access the platform
            </p>
          </div>

          {error && (
            <div className="mb-6 flex items-center gap-2.5 rounded-xl border border-destructive/30 bg-destructive/5 px-4 py-3 text-sm font-medium text-destructive">
              <AlertCircle className="size-4.5 shrink-0" />
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="space-y-1.5">
              <label htmlFor="username" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
                Username or email
              </label>
              <input
                type="text"
                id="username"
                placeholder="priya@vyoog.com"
                autoComplete="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                disabled={submitting}
                className="h-12 w-full rounded-lg border border-input bg-background px-4 text-[15px] shadow-sm transition-shadow placeholder:text-muted-foreground/50 focus:outline-none focus:ring-2 focus:ring-ring/40 disabled:opacity-60"
              />
            </div>

            <div className="space-y-1.5">
              <label htmlFor="password" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
                Password
              </label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  id="password"
                  placeholder="Enter your password"
                  autoComplete="current-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  disabled={submitting}
                  className="h-12 w-full rounded-lg border border-input bg-background px-4 pr-12 text-[15px] shadow-sm transition-shadow placeholder:text-muted-foreground/50 focus:outline-none focus:ring-2 focus:ring-ring/40 disabled:opacity-60"
                />
                <button
                  type="button"
                  className="absolute right-3 top-1/2 -translate-y-1/2 rounded-md p-1.5 text-muted-foreground transition-colors hover:text-foreground"
                  tabIndex={-1}
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                  onClick={() => setShowPassword(!showPassword)}
                >
                  {showPassword ? <EyeOff className="size-4.5" /> : <Eye className="size-4.5" />}
                </button>
              </div>
            </div>

            <div className="flex items-center justify-between">
              <label className="flex cursor-pointer items-center gap-2 text-sm text-muted-foreground">
                <input
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(e) => setRememberMe(e.target.checked)}
                  className="size-4 rounded accent-primary"
                />
                Remember me
              </label>
              <button
                type="button"
                className="text-sm font-medium text-primary transition-colors hover:text-primary/80 hover:underline"
                onClick={onForgotPassword}
              >
                Forgot password?
              </button>
            </div>

            <button
              type="submit"
              className="flex h-[50px] w-full items-center justify-center gap-2 rounded-lg bg-brand-gradient text-[15px] font-semibold text-white shadow-md transition-all hover:-translate-y-0.5 hover:shadow-lg active:translate-y-0 disabled:cursor-not-allowed disabled:opacity-60 disabled:hover:translate-y-0"
              disabled={submitting || !username.trim() || !password}
            >
              {submitting ? (
                <>
                  <Loader2 className="size-4.5 animate-spin" />
                  Signing in…
                </>
              ) : (
                'Sign in'
              )}
            </button>
          </form>

          <div className="mt-9 text-center text-[13px] text-muted-foreground">
            Vyoog Information Private Limited — internal use only
          </div>
        </div>
      </div>
    </div>
  )
}

import { useState } from 'react'
import type { FormEvent } from 'react'
import { apiClient } from '@/api/client'
import { ChevronLeft, Lock, CheckCircle2, Layers, Loader2 } from 'lucide-react'

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
    <div className="grid min-h-screen grid-cols-1 bg-surface-0 lg:grid-cols-2">
      {/* LEFT: Brand panel */}
      <div className="relative hidden flex-col justify-between overflow-hidden bg-brand-gradient p-12 text-white lg:flex">
        <div className="absolute -top-24 -right-24 size-80 rounded-full bg-white/8 blur-3xl" aria-hidden="true" />
        <div className="absolute -bottom-32 -left-24 size-72 rounded-full bg-white/6 blur-3xl" aria-hidden="true" />

        <div className="relative flex items-center gap-3.5">
          <div className="flex size-11 items-center justify-center rounded-xl bg-white/15 backdrop-blur-sm">
            <Layers className="size-5.5" />
          </div>
          <span className="text-[22px] font-bold tracking-tight">ProspectSoul</span>
        </div>

        <div className="relative max-w-sm space-y-5">
          <h1 className="text-4xl font-bold leading-[1.15] tracking-tight">
            Never lose track of a prospect again
          </h1>
          <p className="text-[15px] leading-relaxed text-white/70">
            Every company, every research finding, every qualification decision — tracked permanently with evidence you can verify in seconds.
          </p>
        </div>

        <div className="relative h-1" />
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

          {!submitted ? (
            <div>
              <button
                type="button"
                className="mb-9 inline-flex items-center gap-1.5 text-sm text-muted-foreground transition-colors hover:text-foreground"
                onClick={onBack}
              >
                <ChevronLeft className="size-4" />
                Back to sign in
              </button>

              <div className="mb-6 flex size-14 items-center justify-center rounded-2xl bg-primary/8">
                <Lock className="size-6 text-primary" />
              </div>

              <div className="mb-10">
                <h2 className="text-[28px] font-bold tracking-tight">Reset your password</h2>
                <p className="mt-2 text-[15px] leading-relaxed text-muted-foreground">
                  Enter the email address linked to your account and we'll send you a link to reset your password.
                </p>
              </div>

              <form onSubmit={handleSubmit} className="space-y-5">
                <div className="space-y-1.5">
                  <label htmlFor="forgot-email" className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
                    Email address
                  </label>
                  <input
                    type="email"
                    id="forgot-email"
                    placeholder="priya@vyoog.com"
                    autoComplete="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    disabled={submitting}
                    className="h-12 w-full rounded-lg border border-input bg-background px-4 text-[15px] shadow-sm transition-shadow placeholder:text-muted-foreground/50 focus:outline-none focus:ring-2 focus:ring-ring/40 disabled:opacity-60"
                  />
                </div>

                <button
                  type="submit"
                  className="flex h-[50px] w-full items-center justify-center gap-2 rounded-lg bg-brand-gradient text-[15px] font-semibold text-white shadow-md transition-all hover:-translate-y-0.5 hover:shadow-lg active:translate-y-0 disabled:cursor-not-allowed disabled:opacity-60 disabled:hover:translate-y-0"
                  disabled={submitting || !email.trim()}
                >
                  {submitting ? (
                    <>
                      <Loader2 className="size-4.5 animate-spin" />
                      Sending…
                    </>
                  ) : (
                    'Send reset link'
                  )}
                </button>
              </form>

              <div className="mt-9 text-center text-[13px] text-muted-foreground">
                Vyoog Information Private Limited — internal use only
              </div>
            </div>
          ) : (
            <div>
              <button
                type="button"
                className="mb-9 inline-flex items-center gap-1.5 text-sm text-muted-foreground transition-colors hover:text-foreground"
                onClick={onBack}
              >
                <ChevronLeft className="size-4" />
                Back to sign in
              </button>

              <div className="mb-6 flex size-16 items-center justify-center rounded-full bg-accent-emerald/10 animate-scale-in">
                <CheckCircle2 className="size-7 text-accent-emerald" />
              </div>

              <h2 className="text-2xl font-bold tracking-tight">Check your email</h2>
              <p className="mt-2 text-[15px] leading-relaxed text-muted-foreground">
                We've sent a password reset link to <span className="font-semibold text-foreground">{submittedEmail}</span>
              </p>
              <p className="mt-5 text-[13px] text-muted-foreground">
                Didn't receive it? Check your spam folder or try again in a few minutes.
              </p>

              <button
                type="button"
                className="mt-7 inline-flex h-11 items-center justify-center rounded-lg border border-border bg-card px-6 text-sm font-medium transition-colors hover:bg-surface-1"
                onClick={handleTryAgain}
              >
                Try a different email
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { AuthContext } from './AuthContext'
import type { AuthContextValue, AuthUser, LoginFn } from './AuthContext'
import { initializeKeycloak, keycloak } from './keycloak'
import { setAuthTokenProvider, setOnUnauthorized } from '@/api/client'
import { env } from '@/constants/env'
import { LoginPage } from './LoginPage'
import { ForgotPasswordPage } from './ForgotPasswordPage'

interface TokenSet {
  access_token: string
  refresh_token: string
  expires_in: number
  id_token?: string
}

function parseJwt(token: string): Record<string, unknown> {
  const base64 = token.split('.')[1]!.replace(/-/g, '+').replace(/_/g, '/')
  return JSON.parse(atob(base64))
}

function extractUser(accessToken: string): AuthUser {
  const claims = parseJwt(accessToken)
  return {
    id: (claims.sub as string) ?? '',
    username: (claims.preferred_username as string) ?? '',
    fullName: (claims.name as string) ?? '',
    email: (claims.email as string) ?? '',
  }
}

function extractRoles(accessToken: string): string[] {
  const claims = parseJwt(accessToken)
  const realmAccess = claims.realm_access as { roles?: string[] } | undefined
  if (!realmAccess?.roles) return []
  return realmAccess.roles.filter((r) => r.startsWith('PS_'))
}

const TOKEN_ENDPOINT = `${env.keycloakUrl}/realms/${env.keycloakRealm}/protocol/openid-connect/token`
const LOGOUT_ENDPOINT = `${env.keycloakUrl}/realms/${env.keycloakRealm}/protocol/openid-connect/logout`

async function refreshTokens(refreshToken: string): Promise<TokenSet> {
  const body = new URLSearchParams({
    grant_type: 'refresh_token',
    client_id: env.keycloakClientId,
    refresh_token: refreshToken,
  })
  const res = await fetch(TOKEN_ENDPOINT, { method: 'POST', body })
  if (!res.ok) throw new Error('refresh failed')
  return (await res.json()) as TokenSet
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [tokens, setTokens] = useState<TokenSet | null>(null)
  const [loading, setLoading] = useState(true)
  const [initError, setInitError] = useState<string | null>(null)
  const [authPage, setAuthPage] = useState<'login' | 'forgot-password'>('login')
  const refreshTimer = useRef<ReturnType<typeof setTimeout>>(undefined)

  const startRefreshCycle = useCallback(function schedule(tokenSet: TokenSet) {
    if (refreshTimer.current) clearTimeout(refreshTimer.current)
    const refreshIn = Math.max((tokenSet.expires_in - 60) * 1000, 10_000)
    refreshTimer.current = setTimeout(async () => {
      try {
        const newTokens = await refreshTokens(tokenSet.refresh_token)
        setTokens(newTokens)
        schedule(newTokens)
      } catch {
        setTokens(null)
      }
    }, refreshIn)
  }, [])

  useEffect(() => {
    initializeKeycloak()
      .then((auth) => {
        if (auth && keycloak.token && keycloak.refreshToken) {
          const kcTokens: TokenSet = {
            access_token: keycloak.token,
            refresh_token: keycloak.refreshToken,
            expires_in: Math.floor((keycloak.tokenParsed?.exp ?? 0) - Date.now() / 1000),
          }
          setTokens(kcTokens)
          startRefreshCycle(kcTokens)
        }
        setLoading(false)
      })
      .catch((err: Error) => {
        setInitError(err?.message ?? 'Failed to connect to authentication server')
        setLoading(false)
      })

    return () => {
      if (refreshTimer.current) clearTimeout(refreshTimer.current)
    }
  }, [startRefreshCycle])

  useEffect(() => {
    setAuthTokenProvider(() => tokens?.access_token)
    setOnUnauthorized(() => {
      if (refreshTimer.current) clearTimeout(refreshTimer.current)
      setTokens(null)
    })
  }, [tokens])

  const login: LoginFn = useCallback(async (username, password) => {
    try {
      const body = new URLSearchParams({
        grant_type: 'password',
        client_id: env.keycloakClientId,
        username,
        password,
        scope: 'openid',
      })
      const res = await fetch(TOKEN_ENDPOINT, { method: 'POST', body })
      if (!res.ok) {
        const err = await res.json().catch(() => ({}))
        if (res.status === 401 || err.error === 'invalid_grant') {
          return { success: false, error: 'Invalid username or password' }
        }
        return { success: false, error: err.error_description ?? 'Authentication failed' }
      }
      const tokenSet = (await res.json()) as TokenSet
      setTokens(tokenSet)
      startRefreshCycle(tokenSet)
      return { success: true }
    } catch {
      return { success: false, error: 'Unable to connect to authentication server' }
    }
  }, [startRefreshCycle])

  const authenticated = tokens !== null

  const user = useMemo<AuthUser | null>(() => {
    if (!tokens) return null
    return extractUser(tokens.access_token)
  }, [tokens])

  const roles = useMemo<string[]>(() => {
    if (!tokens) return []
    return extractRoles(tokens.access_token)
  }, [tokens])

  const hasRole = useCallback((role: string) => roles.includes(role), [roles])

  const logout = useCallback(async () => {
    if (refreshTimer.current) clearTimeout(refreshTimer.current)
    if (tokens?.refresh_token) {
      try {
        const body = new URLSearchParams({
          client_id: env.keycloakClientId,
          refresh_token: tokens.refresh_token,
        })
        await fetch(LOGOUT_ENDPOINT, { method: 'POST', body })
      } catch { /* best-effort */ }
    }
    setTokens(null)
    window.location.href = '/'
  }, [tokens])

  const value = useMemo<AuthContextValue>(
    () => ({ authenticated, user, roles, token: tokens?.access_token, hasRole, logout }),
    [authenticated, user, roles, tokens, hasRole, logout],
  )

  if (loading) {
    return (
      <div className="flex h-svh items-center justify-center bg-[#f7f8f6]">
        <div className="text-center">
          <div className="mx-auto mb-4 h-8 w-8 animate-spin rounded-full border-4 border-[#c27a3e] border-t-transparent" />
          <p className="text-sm text-[#5e6360]">Loading...</p>
        </div>
      </div>
    )
  }

  if (initError) {
    return (
      <div className="flex h-svh items-center justify-center bg-[#f7f8f6]">
        <div className="text-center">
          <p className="mb-2 text-lg font-semibold text-[#c93b3b]">Authentication Error</p>
          <p className="text-sm text-[#5e6360]">{initError}</p>
          <button
            onClick={() => window.location.reload()}
            className="mt-4 rounded-md bg-[#c27a3e] px-4 py-2 text-sm font-medium text-white hover:bg-[#d4944f]"
          >
            Retry
          </button>
        </div>
      </div>
    )
  }

  if (!authenticated) {
    if (authPage === 'forgot-password') {
      return <ForgotPasswordPage onBack={() => setAuthPage('login')} />
    }
    return <LoginPage onLogin={login} onForgotPassword={() => setAuthPage('forgot-password')} />
  }

  return <AuthContext value={value}>{children}</AuthContext>
}
